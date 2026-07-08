package com.mytravel.user.service;

import com.mytravel.user.User;
import com.mytravel.user.dto.LoginRequest;
import com.mytravel.user.dto.LoginResponse;
import com.mytravel.user.dto.RegisterRequest;
import com.mytravel.user.dto.UserDto;
import com.mytravel.user.repository.UserRepository;
import com.mytravel.user.service.UserMapper;
import com.mytravel.common.util.EmailCodeCache;
import com.mytravel.common.util.EmailValidator;
import com.mytravel.common.util.JwtUtil;
import com.mytravel.common.util.RateLimiter;
import com.mytravel.user.dto.LoginRequest;
import com.mytravel.user.dto.LoginResponse;
import com.mytravel.user.dto.RegisterRequest;
import com.mytravel.user.dto.ResetPasswordRequest;
import com.mytravel.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailValidator emailValidator;
    private final EmailCodeCache emailCodeCache;
    private final EmailService emailService;
    private final RateLimiter rateLimiter;

    /** 昵称格式: 2-20位，仅允许英文大小写、数字、下划线、连字符、点号 */
    private static final String NICKNAME_REGEX = "^[a-zA-Z0-9._-]{2,20}$";

    /**
     * 发送注册验证码（公开接口，无需登录）
     */
    public void sendRegistrationCode(String email, String ip) {
        // 限流：同一 IP 每 120 秒最多发 1 次
        if (!rateLimiter.tryAcquireInterval("reg-ip-interval:" + ip, 120_000)) {
            throw new RuntimeException("操作太频繁，请2分钟后再试");
        }

        // 限流：同一 IP 每小时最多发 3 次
        if (!rateLimiter.tryAcquireDaily("reg-ip-hourly:" + ip, 60 * 60_000, 3)) {
            throw new RuntimeException("该设备今日发送次数已达上限，请明天再试");
        }

        // 限流：同一邮箱 120 秒内只能发一次
        if (!rateLimiter.tryAcquireInterval("reg-email:" + email, 120_000)) {
            throw new RuntimeException("该邮箱已发送过验证码，请2分钟后再试");
        }

        // 限流：同一邮箱每天最多发 3 次
        if (!rateLimiter.tryAcquireDaily("reg-daily:" + email, 24 * 60 * 60_000, 3)) {
            throw new RuntimeException("该邮箱今日验证码发送次数已达上限，请明天再试");
        }

        if (!emailValidator.isAllowed(email)) {
            throw new RuntimeException("该邮箱域名不被支持，请使用主流邮箱");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("该邮箱已被注册");
        }

        String code = String.format("%06d", new Random().nextInt(999999));
        emailCodeCache.put(email, code, 5 * 60 * 1000);  // 5 分钟有效
        emailService.sendVerificationCode(email, code);
    }

    /**
     * 邮箱注册（公开接口）
     */
    public UserDto register(RegisterRequest request) {
        // 1. 校验邮箱域名
        if (!emailValidator.isAllowed(request.getEmail())) {
            throw new RuntimeException("该邮箱域名不被支持，请使用主流邮箱");
        }

        // 2. 校验邮箱未被注册
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("该邮箱已被注册");
        }

        // 3. 校验验证码
        if (!emailCodeCache.validate(request.getEmail(), request.getCode())) {
            throw new RuntimeException("验证码无效或已过期");
        }

        // 4. 校验昵称格式
        if (request.getNickname() == null || request.getNickname().isBlank()) {
            throw new RuntimeException("昵称不能为空");
        }
        if (!request.getNickname().matches(NICKNAME_REGEX)) {
            throw new RuntimeException("昵称格式不正确（2-20位，仅允许英文字母、数字、下划线、连字符、点号）");
        }

        // 5. 创建用户（用户名取邮箱前缀，若重复则追加随机数）
        String username = generateUsername(request.getEmail());

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setAvatar("/photo/default-avatar.png");
        user.setStatus(0);

        user = userRepository.save(user);
        emailCodeCache.remove(request.getEmail());
        return UserMapper.toDto(user);
    }

    /**
     * 登录（支持邮箱或用户名）
     */
    public LoginResponse login(LoginRequest request) {
        String account = request.getAccount();

        // 先按用户名查，再按邮箱查
        User user = userRepository.findByUsername(account)
                .or(() -> userRepository.findByEmail(account))
                .orElseThrow(() -> new BadCredentialsException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        // 每次登录，token版本号+1，使旧token失效
        user.setTokenVersion(user.getTokenVersion() == null ? 1 : user.getTokenVersion() + 1);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTokenVersion());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUser(UserMapper.toDto(user));
        return response;
    }

    /**
     * 退出登录：使当前 token 失效
     */
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setTokenVersion(user.getTokenVersion() == null ? 1 : user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadCredentialsException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() == null ? 1 : user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    /**
     * 发送密码重置验证码（公开接口，无需登录）
     */
    public void sendResetPasswordCode(String email, String ip) {
        // 限流：同一 IP 每 120 秒最多发 1 次
        if (!rateLimiter.tryAcquireInterval("reset-ip-interval:" + ip, 120_000)) {
            throw new RuntimeException("操作太频繁，请2分钟后再试");
        }

        // 限流：同一 IP 每小时最多发 3 次
        if (!rateLimiter.tryAcquireDaily("reset-ip-hourly:" + ip, 60 * 60_000, 3)) {
            throw new RuntimeException("该设备今日发送次数已达上限，请明天再试");
        }

        // 限流：同一邮箱 120 秒内只能发一次
        if (!rateLimiter.tryAcquireInterval("reset-email:" + email, 120_000)) {
            throw new RuntimeException("该邮箱已发送过验证码，请2分钟后再试");
        }

        // 限流：同一邮箱每天最多发 3 次
        if (!rateLimiter.tryAcquireDaily("reset-daily:" + email, 24 * 60 * 60_000, 3)) {
            throw new RuntimeException("该邮箱今日验证码发送次数已达上限，请明天再试");
        }

        // 校验邮箱域名
        if (!emailValidator.isAllowed(email)) {
            throw new RuntimeException("该邮箱域名不被支持，请使用主流邮箱");
        }

        // 邮箱必须已注册（与注册流程相反）
        if (!userRepository.existsByEmail(email)) {
            throw new RuntimeException("该邮箱未注册");
        }

        String code = String.format("%06d", new Random().nextInt(999999));
        emailCodeCache.put(email, code, 5 * 60 * 1000);  // 5 分钟有效
        emailService.sendResetPasswordCode(email, code);
    }

    /**
     * 通过验证码重置密码（公开接口，无需登录）
     */
    public void resetPassword(ResetPasswordRequest request) {
        // 1. 校验邮箱域名
        if (!emailValidator.isAllowed(request.getEmail())) {
            throw new RuntimeException("该邮箱域名不被支持，请使用主流邮箱");
        }

        // 2. 校验邮箱已注册
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("该邮箱未注册"));

        // 3. 校验验证码
        if (!emailCodeCache.validate(request.getEmail(), request.getCode())) {
            throw new RuntimeException("验证码无效或已过期");
        }

        // 4. 校验新密码长度
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new RuntimeException("新密码至少6位");
        }

        // 5. 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // 6. 使所有旧 token 失效（强制所有设备重新登录）
        user.setTokenVersion(user.getTokenVersion() == null ? 1 : user.getTokenVersion() + 1);
        userRepository.save(user);

        // 7. 清除验证码（防止重用）
        emailCodeCache.remove(request.getEmail());
    }

    /**
     * 根据邮箱生成用户名（取 @ 前部分，若已存在则追加 4 位随机数）
     */
    private String generateUsername(String email) {
        String prefix = email.substring(0, email.indexOf("@")).replaceAll("[^a-zA-Z0-9]", "");
        if (prefix.isEmpty()) {
            prefix = "user";
        }
        if (!userRepository.existsByUsername(prefix)) {
            return prefix;
        }
        // 追加随机数避免重名
        String suffix = String.format("%04d", new Random().nextInt(9999));
        return prefix + suffix;
    }
}

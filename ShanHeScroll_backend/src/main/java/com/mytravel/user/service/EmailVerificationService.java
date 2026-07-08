package com.mytravel.user.service;
import com.mytravel.user.User;
import com.mytravel.user.repository.UserRepository;
import com.mytravel.user.dto.BindEmailRequest;
import com.mytravel.user.dto.SendCodeRequest;

import com.mytravel.user.User;
import com.mytravel.common.util.EmailCodeCache;
import com.mytravel.common.util.EmailValidator;
import com.mytravel.common.util.RateLimiter;
import com.mytravel.user.dto.BindEmailRequest;
import com.mytravel.user.dto.SendCodeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailValidator emailValidator;
    private final EmailCodeCache emailCodeCache;
    private final EmailService emailService;
    private final RateLimiter rateLimiter;

    public void sendVerificationCode(String username, SendCodeRequest request) {
        String email = request.getEmail();

        // 限流：同一用户 60 秒内只能发一次
        if (!rateLimiter.tryAcquireInterval("user:" + username, 60_000)) {
            throw new RuntimeException("操作太频繁，请60秒后再试");
        }

        // 限流：同一邮箱 60 秒内只能发一次
        if (!rateLimiter.tryAcquireInterval("email:" + email, 60_000)) {
            throw new RuntimeException("该邮箱已发送过验证码，请60秒后再试");
        }

        // 限流：同一用户每天最多发 10 次
        if (!rateLimiter.tryAcquireDaily("daily:" + username, 24 * 60 * 60_000, 10)) {
            throw new RuntimeException("今日验证码发送次数已达上限（10次），请明天再试");
        }

        if (!emailValidator.isAllowed(email)) {
            throw new RuntimeException("该邮箱域名不被支持，请使用主流邮箱");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("该邮箱已被其他账号绑定");
        }

        String code = String.format("%06d", new Random().nextInt(999999));
        emailCodeCache.put(email, code, 5 * 60 * 1000);
        emailService.sendVerificationCode(email, code);
    }

    public void bindEmail(String username, BindEmailRequest request) {
        if (!emailCodeCache.validate(request.getEmail(), request.getCode())) {
            throw new RuntimeException("验证码无效或已过期");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("该邮箱已被其他账号绑定");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setEmail(request.getEmail());
        userRepository.save(user);
        emailCodeCache.remove(request.getEmail());
    }
}

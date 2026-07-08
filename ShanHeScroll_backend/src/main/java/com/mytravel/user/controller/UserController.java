package com.mytravel.user.controller;

import com.mytravel.common.ApiResponse;
import com.mytravel.user.dto.BindEmailRequest;
import com.mytravel.user.dto.BindPhoneRequest;
import com.mytravel.user.dto.ChangePasswordRequest;
import com.mytravel.user.dto.LoginRequest;
import com.mytravel.user.dto.LoginResponse;
import com.mytravel.user.dto.RegisterRequest;
import com.mytravel.user.dto.ResetPasswordRequest;
import com.mytravel.user.dto.SendCodeRequest;
import com.mytravel.user.dto.UpdateProfileRequest;
import com.mytravel.user.dto.UserDto;
import com.mytravel.user.service.AuthService;
import com.mytravel.user.service.UserProfileService;
import com.mytravel.user.service.EmailVerificationService;
import com.mytravel.user.dto.BindEmailRequest;
import com.mytravel.user.dto.BindPhoneRequest;
import com.mytravel.user.dto.ChangePasswordRequest;
import com.mytravel.user.dto.LoginRequest;
import com.mytravel.user.dto.LoginResponse;
import com.mytravel.user.dto.RegisterRequest;
import com.mytravel.user.dto.SendCodeRequest;
import com.mytravel.user.dto.UpdateProfileRequest;
import com.mytravel.user.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserProfileService userProfileService;
    private final EmailVerificationService emailVerificationService;

    /**
     * 发送注册验证码（公开接口，无需登录）
     */
    @PostMapping("/register/code")
    public ApiResponse<String> sendRegisterCode(@RequestBody SendCodeRequest request,
                                                 HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        authService.sendRegistrationCode(request.getEmail(), ip);
        return ApiResponse.ok("验证码已发送");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @PostMapping("/password/reset/code")
    public ApiResponse<String> sendResetPasswordCode(@RequestBody SendCodeRequest request,
                                                      HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        authService.sendResetPasswordCode(request.getEmail(), ip);
        return ApiResponse.ok("验证码已发送");
    }

    @PostMapping("/password/reset")
    public ApiResponse<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("密码重置成功，请重新登录");
    }

    @PostMapping("/register")
    public ApiResponse<UserDto> register(@RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(Authentication authentication) {
        authService.logout(authentication.getName());
        return ApiResponse.ok("已退出登录");
    }

    @PutMapping("/password")
    public ApiResponse<String> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        authService.changePassword(authentication.getName(),
                request.getOldPassword(), request.getNewPassword());
        return ApiResponse.ok("密码修改成功");
    }

    @GetMapping("/profile")
    public ApiResponse<UserDto> getProfile(Authentication authentication) {
        return ApiResponse.ok(userProfileService.getProfile(authentication.getName()));
    }

    @PutMapping("/profile")
    public ApiResponse<UserDto> updateProfile(
            @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        return ApiResponse.ok(userProfileService.updateProfile(authentication.getName(), request));
    }

    @PutMapping("/phone/bind")
    public ApiResponse<String> bindPhone(
            @RequestBody BindPhoneRequest request,
            Authentication authentication) {
        userProfileService.bindPhone(authentication.getName(), request);
        return ApiResponse.ok("手机号绑定成功");
    }

    @PostMapping("/email/code")
    public ApiResponse<String> sendCode(
            @RequestBody SendCodeRequest request,
            Authentication authentication) {
        emailVerificationService.sendVerificationCode(authentication.getName(), request);
        return ApiResponse.ok("验证码已发送");
    }

    @PutMapping("/email/bind")
    public ApiResponse<String> bindEmail(
            @RequestBody BindEmailRequest request,
            Authentication authentication) {
        emailVerificationService.bindEmail(authentication.getName(), request);
        return ApiResponse.ok("邮箱绑定成功");
    }
}

package com.mytravel.user;

import com.mytravel.common.util.EmailCodeCache;
import com.mytravel.common.util.EmailValidator;
import com.mytravel.common.util.JwtUtil;
import com.mytravel.common.util.RateLimiter;
import com.mytravel.user.dto.LoginRequest;
import com.mytravel.user.dto.LoginResponse;
import com.mytravel.user.dto.RegisterRequest;
import com.mytravel.user.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailValidator emailValidator;

    @Mock
    private EmailCodeCache emailCodeCache;

    @Mock
    private EmailService emailService;

    @Mock
    private RateLimiter rateLimiter;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil,
                emailValidator, emailCodeCache, emailService, rateLimiter);
    }

    // ---- 注册 ----

    @Test
    void shouldRegisterSuccessfully() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("newuser@qq.com");
        req.setPassword("123456");
        req.setNickname("Traveler");
        req.setCode("123456");

        when(emailValidator.isAllowed("newuser@qq.com")).thenReturn(true);
        when(userRepository.existsByEmail("newuser@qq.com")).thenReturn(false);
        when(emailCodeCache.validate("newuser@qq.com", "123456")).thenReturn(true);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserDto result = authService.register(req);

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("Traveler", result.getNickname());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("existing@qq.com");
        req.setPassword("123456");
        req.setNickname("Test");
        req.setCode("123456");

        when(emailValidator.isAllowed("existing@qq.com")).thenReturn(true);
        when(userRepository.existsByEmail("existing@qq.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(req));
        assertTrue(ex.getMessage().contains("已被注册"));
    }

    @Test
    void shouldRejectInvalidNickname() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@qq.com");
        req.setPassword("123456");
        req.setNickname("包含中文");
        req.setCode("123456");

        when(emailValidator.isAllowed("test@qq.com")).thenReturn(true);
        when(userRepository.existsByEmail("test@qq.com")).thenReturn(false);
        when(emailCodeCache.validate("test@qq.com", "123456")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(req));
        assertTrue(ex.getMessage().contains("昵称格式不正确"));
    }

    // ---- 登录 ----

    @Test
    void shouldLoginWithUsername() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("hashed_password");
        user.setTokenVersion(1);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "hashed_password")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "testuser", 2)).thenReturn("jwt.token.here");

        LoginRequest req = new LoginRequest();
        req.setAccount("testuser");
        req.setPassword("123456");

        LoginResponse resp = authService.login(req);

        assertEquals("jwt.token.here", resp.getToken());
        assertNotNull(resp.getUser());
        assertEquals("testuser", resp.getUser().getUsername());
    }

    @Test
    void shouldLoginWithEmail() {
        User user = new User();
        user.setId(2L);
        user.setUsername("emailuser");
        user.setPassword("hashed_password");
        user.setTokenVersion(0);

        when(userRepository.findByUsername("test@qq.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@qq.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "hashed_password")).thenReturn(true);
        when(jwtUtil.generateToken(2L, "emailuser", 1)).thenReturn("jwt.token.here");

        LoginRequest req = new LoginRequest();
        req.setAccount("test@qq.com");
        req.setPassword("123456");

        LoginResponse resp = authService.login(req);

        assertEquals("jwt.token.here", resp.getToken());
        assertEquals("emailuser", resp.getUser().getUsername());
    }

    @Test
    void shouldRejectWrongPassword() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("hashed_password");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed_password")).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setAccount("testuser");
        req.setPassword("wrong");

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    @Test
    void shouldRejectNonexistentUser() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setAccount("ghost");
        req.setPassword("123456");

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    // ---- 改密 ----

    @Test
    void shouldChangePasswordSuccessfully() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("old_hashed");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "old_hashed")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("new_hashed");

        assertDoesNotThrow(() -> authService.changePassword("testuser", "old", "new"));
    }
}

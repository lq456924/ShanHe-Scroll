package com.mytravel.common.util;

import com.auth0.jwt.exceptions.JWTVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // 通过反射设值（该类使用 @Value 注入）
        setField(jwtUtil, "secret", "test-secret-key-for-junit-testing-2026");
        setField(jwtUtil, "expiration", 60_000L); // 1 分钟
    }

    @Test
    void shouldGenerateAndValidateToken() {
        String token = jwtUtil.generateToken(1L, "testuser", 1);

        assertNotNull(token);
        assertDoesNotThrow(() -> jwtUtil.validateToken(token));
    }

    @Test
    void shouldExtractClaimsFromToken() {
        String token = jwtUtil.generateToken(42L, "alice", 3);

        assertEquals(42L, jwtUtil.getUserIdFromToken(token));
        assertEquals("alice", jwtUtil.getUsernameFromToken(token));
        assertEquals(3, jwtUtil.getTokenVersionFromToken(token));
    }

    @Test
    void shouldRejectTokenWithWrongSecret() {
        String token = jwtUtil.generateToken(1L, "user", 1);

        JwtUtil otherJwt = new JwtUtil();
        setField(otherJwt, "secret", "a-different-secret-key");
        setField(otherJwt, "expiration", 60_000L);

        assertThrows(JWTVerificationException.class,
                () -> otherJwt.validateToken(token));
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtUtil.generateToken(1L, "user", 1);
        String tampered = token.substring(0, token.length() - 5) + "xxxxx";

        assertThrows(JWTVerificationException.class,
                () -> jwtUtil.validateToken(tampered));
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

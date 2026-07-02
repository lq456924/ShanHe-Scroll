package com.mytravel.common.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(Long userId, String username, Integer tokenVersion) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withClaim("tokenVersion", tokenVersion)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expiration))
                .sign(algorithm);
    }

    public DecodedJWT validateToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        JWTVerifier verifier = JWT.require(algorithm).build();
        return verifier.verify(token);
    }

    public Long getUserIdFromToken(String token) {
        return validateToken(token).getClaim("userId").asLong();
    }

    public String getUsernameFromToken(String token) {
        return validateToken(token).getClaim("username").asString();
    }

    public Integer getTokenVersionFromToken(String token) {
        return validateToken(token).getClaim("tokenVersion").asInt();
    }
}

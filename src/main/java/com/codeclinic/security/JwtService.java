package com.codeclinic.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration expiration;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-hours:24}") long expirationHours) {
        if (secret.isBlank()) {
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            this.key = Keys.hmacShaKeyFor(random);
        } else {
            if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalArgumentException("JWT_SECRET至少需要32字节；请使用随机生成的密钥");
            }
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.expiration = Duration.ofHours(expirationHours);
    }

    public String createToken(Long userId, String username, String passwordHash) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("passwordVersion", passwordVersion(passwordHash))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    public TokenIdentity parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new TokenIdentity(Long.valueOf(claims.getSubject()), claims.get("passwordVersion", String.class));
    }

    public boolean matchesPasswordVersion(TokenIdentity identity, String passwordHash) {
        return identity.passwordVersion() != null && MessageDigest.isEqual(
                identity.passwordVersion().getBytes(StandardCharsets.US_ASCII),
                passwordVersion(passwordHash).getBytes(StandardCharsets.US_ASCII));
    }

    private String passwordVersion(String passwordHash) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return java.util.HexFormat.of().formatHex(mac.doFinal(passwordHash.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("令牌校验不可用", e);
        }
    }

    public record TokenIdentity(Long userId, String passwordVersion) {}
}

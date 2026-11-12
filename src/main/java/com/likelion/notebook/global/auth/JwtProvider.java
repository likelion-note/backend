package com.likelion.notebook.global.auth;

import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * 우리 서버가 발급하는 토큰 두 종류.
 * - access: 로그인 후 모든 API에 Authorization: Bearer 로 보냄. subject = userId
 * - signup: 구글 로그인은 됐지만 아직 가입 전. 초대코드 입력 화면까지만 씀. subject = googleSub
 */
@Component
public class JwtProvider {

    private static final String TYPE = "typ";
    private static final String ACCESS = "access";
    private static final String SIGNUP = "signup";
    private static final String EMAIL = "email";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration signupTtl;

    public JwtProvider(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.access-token-hours}") long accessHours,
                       @Value("${app.jwt.signup-token-minutes}") long signupMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofHours(accessHours);
        this.signupTtl = Duration.ofMinutes(signupMinutes);
    }

    public String createAccessToken(Long userId) {
        return build(String.valueOf(userId), ACCESS, null, accessTtl);
    }

    public String createSignupToken(String googleSub, String email) {
        return build(googleSub, SIGNUP, email, signupTtl);
    }

    public Long parseAccessToken(String token) {
        Claims claims = parse(token, ACCESS);
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    public GoogleAccount parseSignupToken(String token) {
        Claims claims = parse(token, SIGNUP);
        return new GoogleAccount(claims.getSubject(), claims.get(EMAIL, String.class));
    }

    private String build(String subject, String type, String email, Duration ttl) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(subject)
                .claim(TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl.toMillis()));
        if (email != null) {
            builder.claim(EMAIL, email);
        }
        return builder.signWith(key).compact();
    }

    private Claims parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            if (!expectedType.equals(claims.get(TYPE, String.class))) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /** 구글 계정에서 우리가 쓰는 값 두 개 */
    public record GoogleAccount(String sub, String email) {
    }
}
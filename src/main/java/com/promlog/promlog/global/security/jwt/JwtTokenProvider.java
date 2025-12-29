package com.promlog.promlog.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(long accountId, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.accessExpSeconds());

        return Jwts.builder()
                .subject(String.valueOf(accountId))
                .claim("role", role)
                .claim("typ", "access")   // ✅ access 표시
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(long accountId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.refreshExpSeconds());

        return Jwts.builder()
                .subject(String.valueOf(accountId))
                .claim("typ", "refresh")  // ✅ refresh 표시
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)          // ✅ 여기 이제 오류 안 남
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long parseSubjectAsLong(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    // ✅ refresh 재발급용 (refresh인지 확인까지)
    public long parseRefreshSubjectAsLong(String refreshToken) {
        Claims claims = parseClaims(refreshToken);
        String typ = claims.get("typ", String.class);
        if (!"refresh".equals(typ)) {
            throw new IllegalArgumentException("NOT_REFRESH_TOKEN");
        }
        return Long.parseLong(claims.getSubject());
    }
}

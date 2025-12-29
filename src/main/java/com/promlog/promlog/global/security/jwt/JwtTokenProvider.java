package com.promlog.promlog.global.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final JwtProperties props;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
    }

    public String createAccessToken(long accountId, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.accessExpSeconds());

        return Jwts.builder()
                .subject(String.valueOf(accountId))
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public String createRefreshToken(long accountId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.refreshExpSeconds());

        return Jwts.builder()
                .subject(String.valueOf(accountId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public long parseSubjectAsLong(String token) {
        String sub = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.parseLong(sub);
    }
}

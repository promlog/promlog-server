package com.promlog.promlog.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // REST API면 폼로그인/기본로그인 페이지 필요 없음
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .authorizeHttpRequests(auth -> auth
                        // ✅ 이거 열어줘야 /login으로 안 튐
                        .requestMatchers(
                                "/api/auth/oauth/**",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // 나머지는 일단 전부 허용(초기 개발 편의)
                        // 나중에 JWT 필터 붙이고 authenticated()로 바꾸면 됨
                        .anyRequest().permitAll()
                )
                .build();
    }
}

package com.promlog.promlog.global.config;

import com.promlog.promlog.global.security.jwt.JwtAuthenticationFilter;
import com.promlog.promlog.global.security.jwt.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .authorizeHttpRequests(auth -> auth
                        // auth
                        .requestMatchers("/api/auth/**").permitAll()

                        // swagger
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // ✅ 내가 쓴 프롬프트 (로그인 필요) — 먼저!
                        .requestMatchers(HttpMethod.GET, "/api/prompts/me").authenticated()

                        // ✅ prompts: 공개
                        .requestMatchers(HttpMethod.GET, "/api/prompts/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/prompts/*/copy").permitAll()

                        // ✅ prompts: 로그인 필요
                        .requestMatchers(HttpMethod.POST, "/api/prompts").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/prompts/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/prompts/*").authenticated()

                        // 그 외 정책
                        .anyRequest().authenticated()
                )

                // JWT 필터 등록
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }
}

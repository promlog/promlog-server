package com.promlog.promlog;

import com.promlog.promlog.auth.infra.kakao.KakaoOAuthProperties;
import com.promlog.promlog.global.security.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({JwtProperties.class, KakaoOAuthProperties.class})
@SpringBootApplication
public class PromlogApplication {

	public static void main(String[] args) {
		SpringApplication.run(PromlogApplication.class, args);
	}

}

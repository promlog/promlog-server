package com.promlog.promlog.auth.infra.kakao;

import com.promlog.promlog.auth.infra.kakao.dto.KakaoTokenResponse;
import com.promlog.promlog.auth.infra.kakao.dto.KakaoUserInfoResponse;
import com.promlog.promlog.global.error.BusinessException;
import com.promlog.promlog.global.error.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class KakaoClient {

    private final WebClient webClient;
    private final KakaoOAuthProperties props;

    public KakaoClient(KakaoOAuthProperties props) {
        this.props = props;
        this.webClient = WebClient.builder().build();
    }

    public KakaoTokenResponse exchangeToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", props.clientId());
        form.add("redirect_uri", props.redirectUri());
        form.add("code", code);

        // client_secret은 사용 안 함

        try {
            return webClient.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(form)
                    .retrieve()
                    .bodyToMono(KakaoTokenResponse.class)
                    .block();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "카카오 토큰 교환에 실패했습니다.", e.getMessage());
        }
    }

    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        try {
            return webClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(KakaoUserInfoResponse.class)
                    .block();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "카카오 유저 정보 조회에 실패했습니다.", e.getMessage());
        }
    }
}

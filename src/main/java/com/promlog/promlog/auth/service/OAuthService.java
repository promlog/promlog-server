package com.promlog.promlog.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promlog.promlog.account.domain.Account;
import com.promlog.promlog.account.domain.AccountStatus;
import com.promlog.promlog.account.repository.AccountRepository;
import com.promlog.promlog.auth.dto.AuthResponse;
import com.promlog.promlog.auth.infra.kakao.KakaoClient;
import com.promlog.promlog.auth.infra.kakao.dto.KakaoUserInfoResponse;
import com.promlog.promlog.global.error.BusinessException;
import com.promlog.promlog.global.error.ErrorCode;
import com.promlog.promlog.global.security.jwt.JwtTokenProvider;
import com.promlog.promlog.oauthidentity.domain.OAuthIdentity;
import com.promlog.promlog.oauthidentity.domain.OAuthProviderType;
import com.promlog.promlog.oauthidentity.repository.OAuthIdentityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class OAuthService {

    private final KakaoClient kakaoClient;
    private final OAuthIdentityRepository oauthRepo;
    private final AccountRepository accountRepo;
    private final JwtTokenProvider jwt;
    private final ObjectMapper objectMapper;

    public OAuthService(
            KakaoClient kakaoClient,
            OAuthIdentityRepository oauthRepo,
            AccountRepository accountRepo,
            JwtTokenProvider jwt,
            ObjectMapper objectMapper
    ) {
        this.kakaoClient = kakaoClient;
        this.oauthRepo = oauthRepo;
        this.accountRepo = accountRepo;
        this.jwt = jwt;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AuthResponse kakaoLogin(String code) {
        final KakaoUserInfoResponse me;
        try {
            var token = kakaoClient.exchangeToken(code);
            me = kakaoClient.getUserInfo(token.accessToken());
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            Throwable root = rootCause(e);
            log.warn("[KakaoOAuthFailed] msg={}", String.valueOf(root.getMessage()));
            throw new BusinessException(
                    ErrorCode.OAUTH_PROVIDER_ERROR,
                    "카카오 로그인 처리 중 오류가 발생했습니다.",
                    Map.of("reason", "KAKAO_API_ERROR")
            );
        }

        String subject = String.valueOf(me.id());
        OAuthProviderType provider = OAuthProviderType.KAKAO;

        var identityOpt = oauthRepo.findByProviderAndSubjectAndDeletedAtIsNull(provider, subject);

        Account account;
        if (identityOpt.isPresent()) {
            account = identityOpt.get().getAccount();
        } else {
            // 신규 가입
            account = new Account(pickNickname(me));
            accountRepo.save(account);

            try {
                oauthRepo.saveAndFlush(new OAuthIdentity(
                        account,
                        provider,
                        subject,
                        pickEmail(me),
                        toProfileJson(me)
                ));
            } catch (RuntimeException e) {
                throw mapOAuthInsertException(e);
            }
        }

        // 토큰 발급 전 차단
        validateAccountStatus(account);

        // 통과한 계정만 last_login_at 갱신
        account.updateLastLoginAt(LocalDateTime.now());
        accountRepo.save(account);

        String access = jwt.createAccessToken(account.getId(), account.getRole().name());
        String refresh = jwt.createRefreshToken(account.getId());

        return new AuthResponse(
                access,
                refresh,
                new AuthResponse.AccountDto(
                        account.getId(),
                        account.getNickname(),
                        account.getRole().name(),
                        account.getStatus()
                )
        );
    }

    private void validateAccountStatus(Account account) {
        LocalDateTime now = LocalDateTime.now();

        if (account.getStatus() == AccountStatus.SUSPENDED) {
            LocalDateTime until = account.getSuspendedUntil();
            if (until != null && now.isBefore(until)) {
                throw new BusinessException(
                        ErrorCode.ACCOUNT_SUSPENDED,
                        "정지된 계정입니다.",
                        Map.of("suspendedUntil", until)
                );
            }
        }

        if (account.getStatus() == AccountStatus.DELETED) {
            LocalDateTime deletedAt = account.getDeletedAt();
            LocalDateTime rejoinAllowedAt = (deletedAt == null) ? null : deletedAt.plusDays(7);

            if (rejoinAllowedAt != null && now.isBefore(rejoinAllowedAt)) {
                throw new BusinessException(
                        ErrorCode.REJOIN_NOT_ALLOWED,
                        "탈퇴 후 7일이 지나야 재가입/로그인이 가능합니다.",
                        Map.of("rejoinAllowedAt", rejoinAllowedAt)
                );
            }

            Map<String, Object> details = new HashMap<>();
            if (rejoinAllowedAt != null) details.put("rejoinAllowedAt", rejoinAllowedAt);
            if (deletedAt != null) details.put("deletedAt", deletedAt);

            throw new BusinessException(
                    ErrorCode.ACCOUNT_DELETED,
                    "탈퇴 처리된 계정입니다. 재가입을 진행해 주세요.",
                    details
            );
        }
    }

    private String pickNickname(KakaoUserInfoResponse me) {
        if (me.properties() != null && me.properties().nickname() != null) return me.properties().nickname();
        return "user-" + me.id();
    }

    private String pickEmail(KakaoUserInfoResponse me) {
        if (me.kakaoAccount() != null) return me.kakaoAccount().email();
        return null;
    }

    private String toProfileJson(KakaoUserInfoResponse me) {
        try {
            return objectMapper.writeValueAsString(me);
        } catch (Exception e) {
            log.warn("[toProfileJson] failed. msg={}", e.getMessage());
            return null;
        }
    }

    private BusinessException mapOAuthInsertException(RuntimeException e) {
        Throwable root = rootCause(e);
        String msg = String.valueOf(root.getMessage());

        if (msg.contains("Rejoin is allowed only after 7 days")) {
            return new BusinessException(
                    ErrorCode.REJOIN_NOT_ALLOWED,
                    "탈퇴 후 7일이 지나야 재가입/로그인이 가능합니다.",
                    Map.of("reason", "REJOIN_NOT_ALLOWED")
            );
        }

        if (msg.contains("already linked")) {
            return new BusinessException(
                    ErrorCode.OAUTH_ALREADY_LINKED,
                    "이미 연결된 소셜 계정입니다.",
                    Map.of("reason", "OAUTH_ALREADY_LINKED")
            );
        }

        return new BusinessException(
                ErrorCode.CONFLICT,
                "소셜 계정 연결 중 오류가 발생했습니다.",
                Map.of("details", msg)
        );
    }

    private Throwable rootCause(Throwable t) {
        Throwable cur = t;
        int guard = 0;
        while (cur.getCause() != null && cur.getCause() != cur && guard++ < 20) {
            cur = cur.getCause();
        }
        return cur;
    }
}
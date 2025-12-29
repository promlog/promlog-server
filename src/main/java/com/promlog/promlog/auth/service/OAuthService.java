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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class OAuthService {

    private final KakaoClient kakaoClient;
    private final OAuthIdentityRepository oauthRepo;
    private final AccountRepository accountRepo;
    private final JwtTokenProvider jwt;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OAuthService(
            KakaoClient kakaoClient,
            OAuthIdentityRepository oauthRepo,
            AccountRepository accountRepo,
            JwtTokenProvider jwt
    ) {
        this.kakaoClient = kakaoClient;
        this.oauthRepo = oauthRepo;
        this.accountRepo = accountRepo;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse kakaoLogin(String code) {
        var token = kakaoClient.exchangeToken(code);
        var me = kakaoClient.getUserInfo(token.accessToken());

        String subject = String.valueOf(me.id());
        OAuthProviderType provider = OAuthProviderType.KAKAO;

        var identityOpt = oauthRepo.findByProviderAndSubjectAndDeletedAtIsNull(provider, subject);

        Account account;
        if (identityOpt.isPresent()) {
            account = identityOpt.get().getAccount();
        } else {
            // 신규 가입
            account = new Account(pickNickname(me));
            accountRepo.saveAndFlush(account);

            try {
                oauthRepo.saveAndFlush(new OAuthIdentity(
                        account,
                        provider,
                        subject,
                        pickEmail(me),
                        toProfileJson(me)
                ));
            } catch (RuntimeException e) {
                // ✅ 트리거(45000) / unique 충돌 / JPA flush 예외 등 전부 여기로 들어올 수 있음
                throw mapOAuthInsertException(e);
            }
        }

        // ✅ 정지/탈퇴/재가입 제한: 토큰 발급 전에 반드시 차단
        validateAccountStatus(account);

        // ✅ 통과한 계정만 last_login_at 갱신
        account.updateLastLoginAt(LocalDateTime.now());

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

            // 7일 제한 중
            if (rejoinAllowedAt != null && now.isBefore(rejoinAllowedAt)) {
                throw new BusinessException(
                        ErrorCode.REJOIN_NOT_ALLOWED,
                        "탈퇴 후 7일이 지나야 재가입/로그인이 가능합니다.",
                        Map.of("rejoinAllowedAt", rejoinAllowedAt)
                );
            }

            // ✅ 7일이 지났어도 DELETED면 로그인 불가. (재가입 유도)
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
            return null;
        }
    }

    /**
     * ✅ DB 트리거(45000) / Unique / Constraint / JPA flush 에러 등
     * DataIntegrityViolationException으로 안 들어오고 JpaSystemException 등으로도 올 수 있어서
     * RuntimeException으로 받아서 root cause 메시지로 매핑한다.
     */
    private BusinessException mapOAuthInsertException(RuntimeException e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }

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

        Map<String, Object> details = new HashMap<>();
        details.put("details", msg);

        return new BusinessException(
                ErrorCode.CONFLICT,
                "소셜 계정 연결 중 오류가 발생했습니다.",
                details
        );
    }
}

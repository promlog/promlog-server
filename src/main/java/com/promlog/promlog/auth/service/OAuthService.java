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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

        // 1) 기존 identity 있으면 account 로그인
        var identityOpt = oauthRepo.findByProviderAndSubjectAndDeletedAtIsNull(provider, subject);

        Account account;
        if (identityOpt.isPresent()) {
            account = identityOpt.get().getAccount();
        } else {
            // 2) 없으면 account + identity 생성
            account = new Account(pickNickname(me));
            accountRepo.save(account);

            try {
                oauthRepo.save(new OAuthIdentity(
                        account,
                        provider,
                        subject,
                        pickEmail(me),
                        toProfileJson(me)
                ));
            } catch (DataIntegrityViolationException e) {
                // 트리거(45000)나 unique 충돌이 여기로 들어올 가능성 큼
                throw mapOAuthInsertException(e);
            }
        }

        // 3) 계정 상태 검증 (정지/탈퇴)
        validateAccountStatus(account);

        // 4) last_login_at 업데이트
        account.updateLastLoginAt(LocalDateTime.now());

        // 5) JWT 발급
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
        if (account.getStatus() == AccountStatus.SUSPENDED) {
            if (account.getSuspendedUntil() != null && LocalDateTime.now().isBefore(account.getSuspendedUntil())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "정지된 계정입니다.", null);
            }
        }
        if (account.getStatus() == AccountStatus.DELETED) {
            // deleted_at + 7일 이전엔 로그인 불가
            if (account.getDeletedAt() != null && LocalDateTime.now().isBefore(account.getDeletedAt().plusDays(7))) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "탈퇴 후 7일이 지나야 재가입/로그인이 가능합니다.", null);
            }
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

    private BusinessException mapOAuthInsertException(DataIntegrityViolationException e) {
        String msg = String.valueOf(e.getMostSpecificCause().getMessage());
        if (msg.contains("Rejoin is allowed only after 7 days")) {
            return new BusinessException(ErrorCode.FORBIDDEN, "탈퇴 후 7일이 지나야 재가입/로그인이 가능합니다.", null);
        }
        if (msg.contains("already linked")) {
            return new BusinessException(ErrorCode.CONFLICT, "이미 연결된 소셜 계정입니다.", null);
        }
        return new BusinessException(ErrorCode.CONFLICT, "소셜 계정 연결 중 오류가 발생했습니다.", msg);
    }
}

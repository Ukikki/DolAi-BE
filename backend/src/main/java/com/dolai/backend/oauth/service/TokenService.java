package com.dolai.backend.oauth.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.oauth.jwt.Token;
import com.dolai.backend.oauth.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class TokenService {

    private final TokenRepository tokenRepository;

    public void saveOrUpdate(String userId, String refreshToken, String accessToken) {
        Token token = tokenRepository.findById(userId)
                .map(existing -> {
                    log.info("🔄 Redis에서 기존 토큰 업데이트: userId={}", userId);
                    existing.updateAccessToken(accessToken);
                    return existing.updateRefreshToken(refreshToken);
                })
                .orElseGet(() -> {
                    log.info("🆕 Redis에 새 토큰 저장: userId={}", userId);
                    return new Token(userId, refreshToken, accessToken);
                });

        tokenRepository.save(token);
        log.info("✅ [TokenService] 저장 완료: userId={}, refreshToken={}, accessToken={}", userId, refreshToken, accessToken);
    }

    public Token findByUserIdOrThrow(String userId) {
        return tokenRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("❌ [TokenService] Redis에서 토큰 못 찾음: userId={}", userId);
                    return new CustomException(ErrorCode.TOKEN_NOT_FOUND);
                });
    }

    public void updateToken(String userId, String newAccessToken) {
        Token token = findByUserIdOrThrow(userId);
        token.updateAccessToken(newAccessToken);
        tokenRepository.save(token);
        log.info("🔁 AccessToken 갱신 완료: userId={}, newAccessToken={}", userId, newAccessToken);
    }

    public String findRefreshTokenByUserId(String userId) {
        Token token = tokenRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TOKEN_NOT_FOUND));
        return token.getRefreshToken();
    }
}

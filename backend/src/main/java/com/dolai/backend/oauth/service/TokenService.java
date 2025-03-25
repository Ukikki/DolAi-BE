package com.dolai.backend.oauth.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.oauth.jwt.Token;
import com.dolai.backend.oauth.repository.TokenRepository;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class TokenService {

    private final TokenRepository tokenRepository;

    public void deleteRefreshToken(Long memberKey) {
        if (tokenRepository.existsById(memberKey)) {
            tokenRepository.deleteById(memberKey);
            log.info("Deleted refresh token for memberKey={}", memberKey);
        } else {
            log.warn("Attempted to delete non-existing refresh token for memberKey={}", memberKey);
        }
    }

    @Transactional
    public void saveOrUpdate(User user, String refreshToken, String accessToken) {
        Token token = tokenRepository.findByUser(user) // User 기준 조회
                .map(existingToken -> {
                    existingToken.updateAccessToken(accessToken);  // AccessToken 업데이트
                    existingToken.updateRefreshToken(refreshToken); // RefreshToken 업데이트
                    log.info("Updated token for userId={}", user.getId());
                    return existingToken;
                })
                .orElseGet(() -> {
                    log.info("Creating new token for userId={}", user.getId());
                    return new Token(user, accessToken, refreshToken);
                });

        tokenRepository.save(token);
    }

    // AccessToken을 이용해 Token을 찾거나 예외를 던짐
    public Token findByAccessTokenOrThrow(String accessToken) {
        return tokenRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new CustomException(ErrorCode.TOKEN_NOT_FOUND));
    }

    @Transactional
    public void updateToken(String accessToken, Token token) {
        token.updateAccessToken(accessToken);
        String userId = token.getUser().getId(); // 올바른 유저 ID 가져오기
        log.info("Updated access token for userId={}", userId);
        tokenRepository.save(token);
    }
}

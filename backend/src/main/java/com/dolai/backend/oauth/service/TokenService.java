package com.dolai.backend.oauth.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.oauth.jwt.Token;
import com.dolai.backend.oauth.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class TokenService {

    private final TokenRepository tokenRepository;

    @Transactional
    public void saveOrUpdate(String userId, String refreshToken, String accessToken) {
        Token token = tokenRepository.findById(userId)
                .map(t -> {
                    t.updateAccessToken(accessToken);
                    return t.updateRefreshToken(refreshToken);
                })
                .orElseGet(() -> new Token(userId, refreshToken, accessToken));
        tokenRepository.save(token);
    }

    public Token findByUserIdOrThrow(String userId) {
        return tokenRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TOKEN_NOT_FOUND));
    }

    @Transactional
    public void updateToken(String userId, String newAccessToken) {
        Token token = findByUserIdOrThrow(userId);
        token.updateAccessToken(newAccessToken);
        tokenRepository.save(token);
    }
}

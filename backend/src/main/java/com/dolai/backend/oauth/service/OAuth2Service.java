package com.dolai.backend.oauth.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.oauth.model.OAuth2UserInfo;
import com.dolai.backend.oauth.jwt.TokenProvider;
import com.dolai.backend.oauth.model.*;
import com.dolai.backend.user.model.User;
import com.dolai.backend.user.model.UserDto;
import com.dolai.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {
    private final OAuth2ApiClient oAuth2ApiClient;
    private final TokenProvider tokenProvider;
    private final UserService userService;
    private final OAuth2Properties properties;

    public LoginResponseDto authenticate(String provider, String code) {
        log.info("🔹 OAuth2 로그인 시도: provider={}, code={}", provider, code);
        log.info("✅ OAuth2Properties 확인: {}", properties); // properties가 null인지 확인

        OAuth2Properties.Provider providerConfig = properties.getProvider().get(provider);

        if (providerConfig == null) {
            log.error("❌ providerConfig가 null입니다! provider={}", provider);
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        log.info("✅ providerConfig 로드 완료: clientId={}", providerConfig.getClientId());

        // OAuth2 oauthAccessToken 요청
        Map<String, Object> tokenResponse = oAuth2ApiClient.getAccessToken(provider, code);
        String oauthAccessToken = (String) tokenResponse.get("access_token");

        // Access Token을 사용하여 사용자 정보 가져오기
        Map<String, Object> userInfoResponse = oAuth2ApiClient.getUserInfo(provider, oauthAccessToken);
        OAuth2UserInfo userInfo = OAuth2UserInfo.of(provider, userInfoResponse);

        // User 저장 또는 업데이트
        User user = userService.saveOrUpdate(userInfo);
        log.info("✅ 사용자 저장/업데이트 완료: userId={}", user.getId());

        // JWT 생성 (TokenProvider 사용)
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        String jwtAccessToken = tokenProvider.generateAccessToken(authentication);
        String jwtRefreshToken = tokenProvider.generateRefreshToken(authentication, jwtAccessToken);

        UserDto userDto = UserDto.create(user);

        return new LoginResponseDto(userDto, jwtAccessToken, jwtRefreshToken);
    }
}

package com.dolai.backend.oauth.service;

import com.dolai.backend.oauth.info.OAuth2UserInfo;
import com.dolai.backend.oauth.info.GoogleOAuth2UserInfo;
import com.dolai.backend.oauth.info.KakaoOAuth2UserInfo;
import com.dolai.backend.user.domain.enums.Provider;

import java.util.Map;

public class OAuth2UserInfoService {
    public static OAuth2UserInfo getOAuth2UserInfo(String provider, Map<String, Object> attributes) {
        Provider providerEnum;
        try {
            providerEnum = Provider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 OAuth Provider: " + provider);
        }

        switch (providerEnum) {
            case GOOGLE:
                return new GoogleOAuth2UserInfo(attributes);
            case KAKAO:
                return new KakaoOAuth2UserInfo(attributes);
            default:
                throw new IllegalArgumentException("지원하지 않는 OAuth Provider: " + provider);
        }
    }
}

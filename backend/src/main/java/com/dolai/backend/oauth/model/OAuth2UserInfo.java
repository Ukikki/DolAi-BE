package com.dolai.backend.oauth.model;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.user.model.User;
import com.dolai.backend.user.model.enums.Provider;

import java.util.Map;

public interface OAuth2UserInfo {
    String getSub();
    String getEmail();
    String getName();
    String getProfileImageUrl();
    User toEntity();
    Provider getProvider();

    static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        switch (registrationId) {
            case "google":
                return new GoogleOAuth2UserInfo(attributes);
            case "kakao":
                return new KakaoOAuth2UserInfo(attributes);
            default :throw new CustomException(ErrorCode.OAUTH_PROVIDER_NOT_FOUND);
        }
    }
}

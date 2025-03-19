package com.dolai.backend.oauth.info;

import com.dolai.backend.user.domain.User;

public interface OAuth2UserInfo {
    String getEmail();
    String getName();
    String getProfileImageUrl();
    User toEntity();
}

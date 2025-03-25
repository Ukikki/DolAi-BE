package com.dolai.backend.oauth.model;

import com.dolai.backend.user.model.User;
import com.dolai.backend.user.model.enums.Provider;
import com.dolai.backend.user.model.enums.Role;
import lombok.Getter;
import lombok.ToString;
import java.util.Map;

@Getter
@ToString
public class GoogleOAuth2UserInfo implements OAuth2UserInfo {
    private final String sub;
    private final String email;
    private final String name;
    private final String profileImageUrl;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.sub = attributes.get("sub") != null ? (String) attributes.get("sub") : "";  // ✅ null 체크 후 초기화
        this.email = (String) attributes.get("email");
        this.name = (String) attributes.get("name");
        this.profileImageUrl = (String) attributes.get("picture");
    }

    @Override
    public User toEntity() {
        return User.builder()
                .id(sub)
                .email(email)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .provider(Provider.GOOGLE)
                .role(Role.USER)
                .build();
    }

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }
}

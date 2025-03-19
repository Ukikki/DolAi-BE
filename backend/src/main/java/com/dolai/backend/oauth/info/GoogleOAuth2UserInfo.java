package com.dolai.backend.oauth.info;

import com.dolai.backend.user.domain.User;
import com.dolai.backend.user.domain.enums.Provider;
import com.dolai.backend.user.domain.enums.Role;
import lombok.Getter;
import lombok.ToString;
import java.util.Map;

@Getter
@ToString
public class GoogleOAuth2UserInfo implements OAuth2UserInfo {
    private final String email;
    private final String name;
    private final String profileImageUrl;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.email = (String) attributes.get("email");
        this.name = (String) attributes.get("name");
        this.profileImageUrl = (String) attributes.get("picture");
    }

    @Override
    public User toEntity() {
        return User.builder()
                .email(email)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .provider(Provider.GOOGLE)
                .role(Role.USER)
                .build();
    }
}

package com.dolai.backend.oauth.info;

import com.dolai.backend.user.domain.User;
import com.dolai.backend.user.domain.enums.Provider;
import com.dolai.backend.user.domain.enums.Role;
import lombok.Getter;
import lombok.ToString;
import java.util.Map;

@Getter
@ToString
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {
    private final String email;
    private final String name;
    private final String profileImageUrl;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");

        this.email = account != null ? (String) account.get("email") : null;
        this.name = properties != null ? (String) properties.get("nickname") : "Kakao User";
        this.profileImageUrl = properties != null ? (String) properties.get("profile_image") : null;
    }

    @Override
    public User toEntity() {
        return User.builder()
                .email(email)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .provider(Provider.KAKAO)
                .role(Role.USER)
                .build();
    }
}

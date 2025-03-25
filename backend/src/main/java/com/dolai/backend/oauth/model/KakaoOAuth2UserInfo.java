package com.dolai.backend.oauth.model;

import com.dolai.backend.user.model.User;
import com.dolai.backend.user.model.enums.Provider;
import com.dolai.backend.user.model.enums.Role;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Getter
@ToString
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {
    private final String sub;
    private final String email;
    private final String name;
    private final String profileImageUrl;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");

        this.sub = attributes.get("id") != null ? String.valueOf(attributes.get("id")) : "";  // Kakao의 "id"를 sub로 사용
        this.email = (String) account.get("email"); //kakao의 long 타입 id를 String으로
        this.name = (String) properties.get("nickname");
        this.profileImageUrl = properties != null ? (String) properties.get("profile_image") : null;
        log.info("✅ Final Parsed Data - sub: {}, email: {}, name: {}, profileImageUrl: {}",
                this.sub, this.email, this.name, this.profileImageUrl);
    }

    @Override
    public User toEntity() {
        return User.builder()
                .id(sub)
                .email(email)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .provider(Provider.KAKAO)
                .role(Role.USER)
                .build();
    }

    @Override
    public Provider getProvider() {
        return Provider.KAKAO;
    }

}

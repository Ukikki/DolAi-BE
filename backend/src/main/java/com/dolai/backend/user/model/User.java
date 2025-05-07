package com.dolai.backend.user.model;

import com.dolai.backend.common.model.BaseTimeEntity;
import com.dolai.backend.user.model.enums.Language;
import com.dolai.backend.user.model.enums.Provider;
import com.dolai.backend.user.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user")
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {
    @Id
    private String id;  // Google/Kakao에서 받은 고유 sub 값

    @Column(nullable = false, unique = true)
    private String email; // 사용자 이메일 (unique)

    @Column
    private String name; // 사용자 이름

    @Column(name = "profile_image_url")
    private String profileImageUrl; // 프로필 이미지 URL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider; // 가입한 소셜 플랫폼

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Language language = Language.KO;


    public static User create(String id, String email, String name, String profileImageUrl, Provider provider) {
        return User.builder()
                .id(id)
                .email(email)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .provider(provider)
                .role(Role.USER)
                .language(Language.KO)
                .build();
    }


    public User update(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        return this;
    }
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
}

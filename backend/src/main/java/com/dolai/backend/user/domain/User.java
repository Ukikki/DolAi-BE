// MySQL - USER 테이블
package com.dolai.backend.user.domain;

import com.dolai.backend.common.entity.BaseTimeEntity;
import com.dolai.backend.user.domain.enums.Provider;
import com.dolai.backend.user.domain.enums.Role;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user")
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
    private Long id;

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
    private Role role = Role.USER; // 기본값 'user'

    @Builder
    public User(String email, String name, String profileImageUrl, Provider provider, Role role) {
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.provider = provider;
        this.role = role != null ? role : Role.USER; // 기본값 USER 설정
    }

    public User update(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        return this;
    }
    public String getRoleKey() {
        return this.role.name(); // USER -> "USER", ADMIN -> "ADMIN"
    }

}

// 친구 목록을 반환
package com.dolai.backend.friend.model.response;

import com.dolai.backend.user.model.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
public class FriendInfoDto {
    private String id;
    private String email;
    private String name;
    private String profile_image;

    public FriendInfoDto(String id, String email, String name, String profile_image) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.profile_image = profile_image;
    }

    public static FriendInfoDto from(User user) {
        return FriendInfoDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profile_image(user.getProfileImageUrl())
                .build();
    }
}
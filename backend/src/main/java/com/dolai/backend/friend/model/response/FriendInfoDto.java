// 친구 목록을 반환
package com.dolai.backend.friend.model.response;

import com.dolai.backend.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendInfoDto {
    private String id;
    private String email;
    private String name;

    public static FriendInfoDto create(User user) {
        return FriendInfoDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
}
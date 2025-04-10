// 받은 요청 응답 구조
package com.dolai.backend.friend.model.response;

import com.dolai.backend.friend.model.Friends;
import com.dolai.backend.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivedFriendRequestDto {
    private Long requestId;
    private String id;
    private String email;
    private String name;

    public static ReceivedFriendRequestDto from(Friends friends) {
        User requester = friends.getRequester();
        return ReceivedFriendRequestDto.builder()
                .requestId(friends.getId())
                .id(requester.getId())
                .email(requester.getEmail())
                .name(requester.getName())
                .build();
    }
}

// 친구 요청
package com.dolai.backend.friend.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestCreateDto {
    @NotBlank(message = "요청할 대상 userId는 필수입니다.")
    private String targetUserId;
}
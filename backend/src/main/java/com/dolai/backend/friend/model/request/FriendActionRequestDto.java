// accept/reject
package com.dolai.backend.friend.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendActionRequestDto {
    @NotBlank
    private String action;  // "accept" 또는 "reject"
}

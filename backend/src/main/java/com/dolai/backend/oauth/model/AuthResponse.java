package com.dolai.backend.oauth.model;

import com.dolai.backend.user.model.UserDto;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"status", "user"})
public class AuthResponse {
    private final String status;
    private final UserDto user;

    public AuthResponse(String status, UserDto user) {
        this.status = status;
        this.user = user;
    }
}

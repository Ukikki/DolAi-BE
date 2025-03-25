package com.dolai.backend.user.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"id", "email", "name", "profile_image", "token"})
public class UserDto {
    private final String id;
    private final String email;
    private final String name;
    private final String profile_image;
    private final String token;

    public UserDto(String id, String email, String name, String profile_image, String token) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.profile_image = profile_image;
        this.token = token;
    }
}

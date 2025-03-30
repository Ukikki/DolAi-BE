package com.dolai.backend.user.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"id", "email", "name", "profile_image"})
public class UserDto {
    private final String id;
    private final String email;
    private final String name;
    private final String profile_image;

    public UserDto(String id, String email, String name, String profile_image) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.profile_image = profile_image;
    }
}

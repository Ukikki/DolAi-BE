package com.dolai.backend.user.model;

import com.dolai.backend.user.model.enums.Language;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JsonPropertyOrder({"id", "email", "name", "profile_image", "language"})
public class UserDto {
    private final String id;
    private final String email;
    private final String name;
    private final String profile_image;
    private final Language language;

    public static UserDto create(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profile_image(user.getProfileImageUrl())
                .language(user.getLanguage())
                .build();
    }
}
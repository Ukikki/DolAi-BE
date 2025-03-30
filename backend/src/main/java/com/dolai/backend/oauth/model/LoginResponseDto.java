package com.dolai.backend.oauth.model;

import com.dolai.backend.user.model.UserDto;
import lombok.Getter;

@Getter
public class LoginResponseDto {
    private final UserDto user;
    private final Tokens tokens;

    public LoginResponseDto(UserDto user, String accessToken, String refreshToken) {
        this.user = user;
        this.tokens = new Tokens(accessToken, refreshToken);
    }

    @Getter
    private static class Tokens {
        private final String accessToken;
        private final String refreshToken;

        public Tokens(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
}

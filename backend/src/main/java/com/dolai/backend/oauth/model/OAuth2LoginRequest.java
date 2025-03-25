package com.dolai.backend.oauth.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OAuth2LoginRequest {
    private String provider;  // "google" or "kakao"
    private String code;      // Authorization Code

    public OAuth2LoginRequest(String provider, String code) {
        this.provider = provider;
        this.code = code;
    }
}

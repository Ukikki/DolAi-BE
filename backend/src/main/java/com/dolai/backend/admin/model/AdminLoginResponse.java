package com.dolai.backend.admin.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminLoginResponse {
    private String accessToken;
    private String refreshToken;
    private String role;
}
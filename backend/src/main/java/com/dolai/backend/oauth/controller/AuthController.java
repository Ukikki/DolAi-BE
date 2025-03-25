package com.dolai.backend.oauth.controller;

import com.dolai.backend.oauth.model.AuthResponse;
import com.dolai.backend.oauth.model.OAuth2LoginRequest;
import com.dolai.backend.oauth.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/auth")
@RequiredArgsConstructor
@RestController
public class AuthController {
    private final OAuth2Service oauth2Service;

    @PostMapping("/social")
    public ResponseEntity<AuthResponse> socialLogin(@RequestBody OAuth2LoginRequest request) {
        log.info("🔹 소셜 로그인 요청: provider={}, code={}", request.getProvider(), request.getCode());
        return ResponseEntity.ok(oauth2Service.authenticate(request.getProvider(), request.getCode()));
    }
}

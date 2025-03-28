package com.dolai.backend.oauth.controller;

import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.common.exception.ErrorResponse;
import com.dolai.backend.common.success.SuccessResponse;
import com.dolai.backend.oauth.jwt.TokenProvider;
import com.dolai.backend.oauth.model.OAuth2LoginRequest;
import com.dolai.backend.oauth.repository.TokenRepository;
import com.dolai.backend.oauth.service.OAuth2Service;
import com.dolai.backend.user.model.User;
import com.dolai.backend.user.model.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequestMapping("/auth")
@RequiredArgsConstructor
@RestController
public class AuthController {
    private final OAuth2Service oauth2Service;
    private final TokenProvider tokenProvider;
    private final TokenRepository tokenRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @PostMapping("/social")
    public ResponseEntity<?> socialLogin(@RequestBody OAuth2LoginRequest request) {
        log.info("🔹 소셜 로그인 요청: provider={}, code={}", request.getProvider(), request.getCode());
        UserDto userDto = oauth2Service.authenticate(request.getProvider(), request.getCode());
        return ResponseEntity.ok(new SuccessResponse<>(userDto));
    }

    @DeleteMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, @AuthenticationPrincipal User user) {
        String accessToken = resolveToken(request);

        if (!StringUtils.hasText(accessToken)) {
            return ErrorResponse.toResponseEntity(ErrorCode.INVALID_JWT);
        }

        long expiration = tokenProvider.getExpiration(accessToken);
        redisTemplate.opsForValue().set("blacklist:" + accessToken, "logout", expiration, TimeUnit.MILLISECONDS);

        tokenRepository.deleteById(user.getId());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "User logged out successfully"
        ));    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return null;
    }

}

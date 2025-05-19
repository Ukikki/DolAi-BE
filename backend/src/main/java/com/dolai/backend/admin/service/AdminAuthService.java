package com.dolai.backend.admin.service;

import com.dolai.backend.admin.model.AdminLoginRequest;
import com.dolai.backend.admin.model.AdminLoginResponse;
import com.dolai.backend.admin.model.AdminUser;
import com.dolai.backend.admin.repository.AdminUserRepository;
import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    private static final long ONE_DAY = 1000L * 60 * 60 * 24; // 1일

    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminUser admin = adminUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtUtil.generateToken(admin.getUsername(), "ROLE_ADMIN", ONE_DAY);
        String refreshToken = ""; // 또는 generateRefreshToken(...) 구현해도 됨

        return new AdminLoginResponse(accessToken, refreshToken, "ROLE_ADMIN");
    }
}

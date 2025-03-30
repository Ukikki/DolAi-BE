package com.dolai.backend.user.controller;

import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.user.model.User;
import com.dolai.backend.user.model.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
    @GetMapping("/me")
    public ResponseEntity<SuccessDataResponse<UserDto>> getMyInfo(@AuthenticationPrincipal User user) {
        UserDto userDto = new UserDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl()
        );
        return ResponseEntity.ok(new SuccessDataResponse<>(userDto));
    }
}
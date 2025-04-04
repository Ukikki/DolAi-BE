package com.dolai.backend.user.controller;

import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.user.model.User;
import com.dolai.backend.user.model.UserDto;
import com.dolai.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<SuccessDataResponse<UserDto>> getMyInfo(@AuthenticationPrincipal User user) {
        UserDto userDto = UserDto.create(user);
        return ResponseEntity.ok(new SuccessDataResponse<>(userDto));
    }
    @GetMapping("/search")
    public ResponseEntity<?> searchByEmail(@RequestParam(name = "email") String email) {
        UserDto user = userService.findUserByEmail(email);
        return ResponseEntity.ok(new SuccessDataResponse<>(user));
    }
}
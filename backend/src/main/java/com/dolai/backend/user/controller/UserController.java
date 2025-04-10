package com.dolai.backend.user.controller;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.user.model.User;
import com.dolai.backend.user.model.UserDto;
import com.dolai.backend.user.repository.UserRepository;
import com.dolai.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

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

    @PatchMapping("/rename")
    public ResponseEntity<?> renameUser(@AuthenticationPrincipal User user, @RequestBody UserDto request) {
        String newName = request.getName();

        if (newName == null || newName.trim().isEmpty()) {
            throw new CustomException(ErrorCode.USER_INVALID_INPUT_VALUE);
        }

        user.setName(newName.trim());
        userRepository.save(user);

        return ResponseEntity.ok(new SuccessDataResponse<>(Map.of("name", newName)));
    }


    @PatchMapping("/profile")
    public ResponseEntity<?> uploadProfileImage(@AuthenticationPrincipal User user, @RequestPart("image") MultipartFile imageFile) {

        try {
            String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            Path path = Paths.get("uploads/" + fileName);
            Files.copy(imageFile.getInputStream(), path);

            String imageUrl = "/static/" + fileName;
            user.setProfileImageUrl(imageUrl);
            userRepository.save(user);

            return ResponseEntity.ok(new SuccessDataResponse<>(Map.of("profileImage", imageUrl)));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.USER_FILE_UPLOAD_FAILED);
        }
    }
}
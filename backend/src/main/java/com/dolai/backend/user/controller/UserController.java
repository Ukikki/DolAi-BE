package com.dolai.backend.user.controller;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.user.model.User;
import com.dolai.backend.user.model.UserDto;
import com.dolai.backend.user.model.enums.Language;
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
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
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
        List<UserDto> users = userService.findUserByEmail(email); // 부분 일치 검색
        return ResponseEntity.ok(new SuccessDataResponse<>(users));
    }

    @PatchMapping("/rename")
    public ResponseEntity<?> renameUser(@AuthenticationPrincipal User user, @RequestBody Map<String, String> request) {
        String newName = request.get("name");

        if (newName == null || newName.trim().isEmpty()) {
            throw new CustomException(ErrorCode.USER_INVALID_INPUT_VALUE);
        }

        user.setName(newName.trim());
        userRepository.save(user);

        return ResponseEntity.ok(new SuccessDataResponse<>(Map.of("name", newName)));
    }
    
    @PatchMapping("/profile")
    public ResponseEntity<?> uploadProfileImage(@AuthenticationPrincipal User user, @RequestPart("image") MultipartFile imageFile) {

        // backend/uploads 폴더로 절대 경로 지정
        try {
            String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();

            String currentDir = System.getProperty("user.dir");
            String uploadDir;

            if (currentDir.endsWith("backend")) {
                uploadDir = currentDir + "/uploads";
            } else {
                uploadDir = currentDir + "/backend/uploads";
            }

            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/static/" + fileName;
            user.setProfileImageUrl(imageUrl);
            userRepository.save(user);

            return ResponseEntity.ok(new SuccessDataResponse<>(Map.of("profileImage", imageUrl)));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.USER_FILE_UPLOAD_FAILED);
        }
    }

    @PatchMapping("/language")
    public ResponseEntity<?> updateLanguage(@AuthenticationPrincipal User user, @RequestBody Map<String, String> request) {
        String lang = request.get("language");

        if (lang == null) {
            throw new CustomException(ErrorCode.USER_INVALID_INPUT_VALUE);
        }

        try {
            Language newLanguage = Language.valueOf(lang.toUpperCase()); // KO, EN, ZH 등
            user.setLanguage(newLanguage);
            userRepository.save(user);
            return ResponseEntity.ok(new SuccessDataResponse<>(Map.of("language", newLanguage)));
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.USER_INVALID_INPUT_VALUE);
        }
    }

}
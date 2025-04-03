package com.dolai.backend.directory.controller;

import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.common.success.SuccessMessageResponse;
import com.dolai.backend.directory.model.DirectoryListResponseDto;
import com.dolai.backend.directory.model.DirectoryRequestDto;
import com.dolai.backend.directory.model.DirectoryResponseDto;
import com.dolai.backend.directory.service.DirectoryService;
import com.dolai.backend.directory.service.DirectoryUserService;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/directories")
@RequiredArgsConstructor
public class DirectoryController {

    private final DirectoryService directoryService;
    private final DirectoryUserService directoryUserService;

    @PostMapping
    public ResponseEntity<DirectoryResponseDto> createDirectory(
            @RequestBody DirectoryRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        DirectoryResponseDto response = directoryService.createDirectory(request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getChildDirectories(
            @RequestParam(name = "parentDirectoryId", required = false) String parentDirectoryId
    ) {
        List<DirectoryListResponseDto> directories = directoryService.getChildDirectories(parentDirectoryId);
        return ResponseEntity.ok(new SuccessDataResponse<>(directories));
    }

    @DeleteMapping("/{directoryId}")
    public ResponseEntity<SuccessMessageResponse> deleteDirectory(
            @PathVariable(name = "directoryId") Long directoryId,
            @AuthenticationPrincipal User user
    ) {
        directoryService.deleteDirectory(directoryId, user);
        return ResponseEntity.ok(new SuccessMessageResponse("Document deleted successfully"));
    }

    @PatchMapping("/{directoryId}/color")
    public ResponseEntity<?> updateFolderColor(
            @PathVariable Long directoryId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User user
    ) {
        String newColor = request.get("color");
        String userId = user.getId(); // ⚠️ 타입 맞게 필요 시 Long userId = user.getId();

        log.info("[색상 변경 요청] 디렉터리 ID: {}, 유저 ID: {}, 변경 색상: {}", directoryId, userId, newColor);

        try {
            directoryUserService.updateColor(directoryId, userId, newColor);
            log.info("[색상 변경 완료] 디렉터리 ID: {}", directoryId);
            return ResponseEntity.ok().body(new SuccessMessageResponse("디렉터리 색상 변경 완료"));
        } catch (Exception e) {
            log.error("[색상 변경 실패] 에러: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new SuccessMessageResponse("색상 변경 실패: " + e.getMessage()));
        }
    }
}

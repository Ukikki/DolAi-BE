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

    //디렉터리 생성
    @PostMapping
    public ResponseEntity<DirectoryResponseDto> createDirectory(
            @RequestBody DirectoryRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        DirectoryResponseDto response = directoryService.createDirectory(request, user);
        return ResponseEntity.ok(response);
    }

    //내 디렉터리 조회
    @GetMapping("/my")
    public ResponseEntity<?> getMyDirectories(@AuthenticationPrincipal User user) {
        List<DirectoryListResponseDto> directories = directoryService.getMyDirectories(user);
        return ResponseEntity.ok(new SuccessDataResponse<>(directories));
    }

    //특정 디렉터리 ID를 기반으로 하위 디렉터리 조회
    @GetMapping
    public ResponseEntity<?> getChildDirectories(
            @RequestParam(name = "parentDirectoryId", required = false) Long parentDirectoryId,
            @AuthenticationPrincipal User user
    ) {
        List<DirectoryListResponseDto> directories = directoryService.getChildDirectories(parentDirectoryId, user);
        return ResponseEntity.ok(new SuccessDataResponse<>(directories));
    }

    //사용자 소유 디렉터리 삭제
    @DeleteMapping("/{directoryId}")
    public ResponseEntity<SuccessMessageResponse> deleteDirectory(
            @PathVariable(name = "directoryId") Long directoryId,
            @AuthenticationPrincipal User user
    ) {
        directoryService.deleteDirectory(directoryId, user);
        return ResponseEntity.ok(new SuccessMessageResponse("Document deleted successfully"));
    }

    // 디렉터리 색상 변경
    @PatchMapping("/{directoryId}/color")
    public ResponseEntity<?> updateFolderColor(
            @PathVariable(name="directoryId") Long directoryId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User user
    ) {
        String newColor = request.get("color");
        String userId = user.getId();

        try {
            directoryUserService.updateColor(directoryId, userId, newColor);
            return ResponseEntity.ok().body(new SuccessMessageResponse("디렉터리 색상 변경 완료"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new SuccessMessageResponse("색상 변경 실패: " + e.getMessage()));
        }
    }

    //디렉터리 이름 변경
    @PatchMapping("/{directoryId}/name")
    public ResponseEntity<?> updateDirectoryName(
            @PathVariable(name="directoryId") Long directoryId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User user
    ) {
        String newName = request.get("name");
        directoryUserService.updateDirectoryName(directoryId, newName, user);
        return ResponseEntity.ok().body(new SuccessMessageResponse("디렉터리 이름 변경 완료"));
    }
}

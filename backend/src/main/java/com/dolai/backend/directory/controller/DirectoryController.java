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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<?> getChildDirectories(@RequestParam(name = "parentDirectoryId", required = false) String parentDirectoryId) {
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
    @PatchMapping("/directories/{directoryId}/color")
    public ResponseEntity<?> updateFolderColor(
            @PathVariable Long directoryId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User user
    ) {
        String newColor = request.get("color");
        String userId = user.getId();

        directoryUserService.updateColor(directoryId, userId, newColor);
        return ResponseEntity.ok().body(new SuccessMessageResponse("디렉터리 색상 변경 완료"));
    }
}
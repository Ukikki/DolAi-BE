package com.dolai.backend.directory.controller;

import com.dolai.backend.common.success.SuccessResponse;
import com.dolai.backend.directory.model.DirectoryListResponseDto;
import com.dolai.backend.directory.model.DirectoryRequestDto;
import com.dolai.backend.directory.model.DirectoryResponseDto;
import com.dolai.backend.directory.service.DirectoryService;
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
        return ResponseEntity.ok(new SuccessResponse<>(directories));
    }

    @DeleteMapping("/{directoryId}")
    public ResponseEntity<?> deleteDirectory(
            @PathVariable(name = "directoryId") Long directoryId,
            @AuthenticationPrincipal User user
    ) {
        directoryService.deleteDirectory(directoryId, user);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Directory and all documents deleted successfully"
        ));
    }
}


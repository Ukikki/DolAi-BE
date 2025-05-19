package com.dolai.backend.metadata.controller;

import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.metadata.model.DirectoryMetaDataResponseDto;
import com.dolai.backend.metadata.model.DocumentMetaDataResponseDto;
import com.dolai.backend.metadata.service.DirectoryMetaDataService;
import com.dolai.backend.metadata.service.DocumentMetaDataService;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class MetaDataController {

    private final DirectoryMetaDataService metaDataService;
    private final DocumentMetaDataService documentMetaDataService;
    // 메타데이터 조회
    @GetMapping("/directory/{directoryId}/metadata")
    public ResponseEntity<?> getMetaData(@PathVariable Long directoryId, @AuthenticationPrincipal User user) {
        DirectoryMetaDataResponseDto response = metaDataService.getMetaDataByDirectoryId(directoryId, user);
        return ResponseEntity.ok(new SuccessDataResponse<>(response));
    }

    @GetMapping("/document/{documentId}/metadata")
    public ResponseEntity<?> getDocumentMetaData(@PathVariable Long documentId, @AuthenticationPrincipal User user) {
        DocumentMetaDataResponseDto response = documentMetaDataService.getDocumentMetaDataByDocumentId(documentId);
        return ResponseEntity.ok(new SuccessDataResponse<>(response));
    }
}

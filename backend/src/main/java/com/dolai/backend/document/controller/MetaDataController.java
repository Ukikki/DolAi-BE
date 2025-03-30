package com.dolai.backend.document.controller;

import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.document.model.MetaDataResponseDto;
import com.dolai.backend.document.service.MetaDataService;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/documents")
public class MetaDataController {

    private final MetaDataService metaDataService;

    @GetMapping("/{documentId}/metadata")
    public ResponseEntity<SuccessDataResponse<MetaDataResponseDto>> getMetaData(
            @PathVariable("documentId") Long documentId,
            @AuthenticationPrincipal User user
    ) {
        MetaDataResponseDto response = metaDataService.getMetaDataByDocumentId(documentId);
        return ResponseEntity.ok(new SuccessDataResponse<>(response));
    }
}

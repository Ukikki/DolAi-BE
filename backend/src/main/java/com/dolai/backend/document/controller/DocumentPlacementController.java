package com.dolai.backend.document.controller;

import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.document.model.DocumentSummaryDto;
import com.dolai.backend.document.service.DocumentPlacementService;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/directories")
public class DocumentPlacementController {
    private final DocumentPlacementService documentPlacementService;

    @GetMapping("/{directoryId}/documents")
    public ResponseEntity<SuccessDataResponse<Map<String, Object>>> getDocumentsInDirectory(
            @PathVariable("directoryId") Long directoryId, // 여기에 이름 명시
            @AuthenticationPrincipal User user
    ) {
        List<DocumentSummaryDto> documents = documentPlacementService
                .getDocumentsByDirectory(directoryId, user.getId());

        Map<String, Object> responseMap = Map.of("documents", documents);
        return ResponseEntity.ok(new SuccessDataResponse<>(responseMap));
    }
}

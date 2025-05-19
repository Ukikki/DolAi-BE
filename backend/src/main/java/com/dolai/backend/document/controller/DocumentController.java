package com.dolai.backend.document.controller;

import com.dolai.backend.common.success.SuccessMessageResponse;
import com.dolai.backend.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    //문서 삭제
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<SuccessMessageResponse> deleteDocument(
            @PathVariable("documentId") Long documentId
    ) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.ok(new SuccessMessageResponse("Document deleted successfully"));
    }
}

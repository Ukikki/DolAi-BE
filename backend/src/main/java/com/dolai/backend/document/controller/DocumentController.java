package com.dolai.backend.document.controller;

import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.common.success.SuccessMessageResponse;
import com.dolai.backend.document.model.DocumentResponseDto;
import com.dolai.backend.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    // 특정 회의에 대한 문서 조회
    @GetMapping("/meetings/{meetingId}/document")
    public ResponseEntity<?> getDocumentByMeetingId(
            @PathVariable("meetingId") String meetingId
    ) {
        DocumentResponseDto response = documentService.getDocumentByMeetingId(meetingId);
        return ResponseEntity.ok(new SuccessDataResponse<>(response));
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<SuccessMessageResponse> deleteDocument(
            @PathVariable("documentId") Long documentId
    ) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.ok(new SuccessMessageResponse("Document deleted successfully"));
    }
}

package com.dolai.backend.document.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {
/*
    private final DocumentService documentService;

    // 특정 회의에 대한 문서 조회
    @GetMapping("/{meetingId}")
    public ResponseEntity<List<DocumentResponseDto>> getDocuments(@PathVariable Long meetingId) {
        List<DocumentResponseDto> documents = documentService.getDocumentsByMeetingId(meetingId);
        return ResponseEntity.ok(documents);
    }

    // 문서 생성 (수동 요청)
    @PostMapping
    public ResponseEntity<DocumentResponseDto> createDocument(@RequestBody DocumentRequest requestDto) {
        DocumentResponseDto createdDocument = documentService.createDocument(requestDto);
        return ResponseEntity.ok(createdDocument);
    }

 */
}

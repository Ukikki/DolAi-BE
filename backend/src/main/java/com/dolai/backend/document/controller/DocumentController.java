package com.dolai.backend.document.controller;

import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.common.success.SuccessMessageResponse;
import com.dolai.backend.document.model.Document;
import com.dolai.backend.document.service.DocumentService;
import com.dolai.backend.s3.S3Service;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;
    private final S3Service s3Service;

    //문서 삭제
    @DeleteMapping("/{documentId}")
    public ResponseEntity<SuccessMessageResponse> deleteDocument(@PathVariable("documentId") Long documentId, @AuthenticationPrincipal User user) {
        documentService.deleteDocument(documentId, user);
        return ResponseEntity.ok(new SuccessMessageResponse("Document deleted successfully"));
    }

    // docx 파일을 뷰어로 보기
    @GetMapping("/{documentId}/view-office")
    public ResponseEntity<SuccessDataResponse> viewDocxOnline(@PathVariable("documentId") Long documentId, @AuthenticationPrincipal User user) {
        String fileUrl = documentService.getS3FileUrl(documentId, user);
        String officeViewerUrl = "https://view.officeapps.live.com/op/view.aspx?src=" +
                URLEncoder.encode(fileUrl, StandardCharsets.UTF_8);
        return ResponseEntity.ok(new SuccessDataResponse<>(officeViewerUrl));
    }

    // docx 파일을 다운로드
    @GetMapping("/{documentId}/download-docx")
    public ResponseEntity<SuccessDataResponse> downloadDocx(@PathVariable("documentId") Long documentId, @AuthenticationPrincipal User user) {
        String fileUrl = documentService.getS3FileUrl(documentId, user);
        return ResponseEntity.ok(new SuccessDataResponse<>(fileUrl));
    }

    // docx 파일 pdf 변환
    @GetMapping("/{documentId}/view-pdf")
    public ResponseEntity<byte[]> viewAsPdf(@PathVariable("documentId") Long documentId, @AuthenticationPrincipal User user) {
        Document doc = documentService.getById(documentId);
        File docxFile = s3Service.downloadTempFile(doc.getFileUrl()); // S3에서 docx 파일 다운로드
        byte[] pdfBytes = documentService.convertDocxToPdf(docxFile); // docx → PDF 변환
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"document.pdf\"")
                .body(pdfBytes);
    }
}

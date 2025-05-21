package com.dolai.backend.metadata.service;

import com.dolai.backend.document.model.Document;
import com.dolai.backend.metadata.model.DocumentMetaData;
import com.dolai.backend.metadata.model.DocumentMetaDataResponseDto;
import com.dolai.backend.metadata.repository.DocumentMetaDataRepository;
import com.dolai.backend.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentMetaDataService {

    private final DocumentMetaDataRepository documentMetaDataRepository;
    private final S3Service s3Service;  // S3 서비스 주입

    @Transactional
    public DocumentMetaData createAndSaveMetaData(Document document, String summary) {
        String fileType = document.getFileType().name();
        String fileUrl = document.getFileUrl();
        Long fileSize = s3Service.getFileSize(fileUrl);

        // 문서 메타데이터 생성
        DocumentMetaData metaData = DocumentMetaData.builder()
                .document(document)
                .type(fileType)
                .size(fileSize)
                .summary(summary)
                .build();

        return documentMetaDataRepository.save(metaData);
    }

    //문서 ID로 메타데이터를 조회합니다.
    @Transactional(readOnly = true)
    public DocumentMetaDataResponseDto getDocumentMetaDataByDocumentId(Long documentId) {
        DocumentMetaData metaData = documentMetaDataRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalArgumentException("DocumentMetaData not found for document: " + documentId));
        Document document = metaData.getDocument();
        return DocumentMetaDataResponseDto.builder()
                .title(document.getTitle())
                .type(metaData.getType())
                .size(formatFileSize(metaData.getSize()))
                .createdAt(document.getCreatedAt())
                .lastModifiedAt(document.getUpdatedAt())
                .summary(metaData.getSummary())
                .build();
    }

    public String extractExtension(String fileUrl) {
        int lastDot = fileUrl.lastIndexOf(".");
        if (lastDot == -1) return null;
        return fileUrl.substring(lastDot + 1);
    }

    // size 포맷팅
    private String formatFileSize(Long sizeInBytes) {
        if (sizeInBytes == null) return "0 B";
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeInBytes / 1024.0);
        } else if (sizeInBytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", sizeInBytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", sizeInBytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
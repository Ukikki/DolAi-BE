package com.dolai.backend.document.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.document.model.Document;
import com.dolai.backend.document.model.MetaData;
import com.dolai.backend.document.model.MetaDataResponseDto;
import com.dolai.backend.document.repository.MetaDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MetaDataService {

    private final MetaDataRepository metaDataRepository;

    public MetaDataResponseDto getMetaDataByDocumentId(Long documentId) {
        MetaData meta = metaDataRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));

        return MetaDataResponseDto.builder()
                .type(meta.getType())
                .size(meta.getSize())
                .createdAt(meta.getCreatedAt())
                .lastModifiedAt(meta.getUpdatedAt())
                .build();
    }

    public void saveMetaData(Document document) {
        String fileUrl = document.getFileUrl(); // ex: https://.../somefile.pdf

        // 1. 파일 확장자 → type
        String extension = extractExtension(fileUrl);
        String fileType = extension.toLowerCase();

        // 파일 크기 구하기 (예시용, 실제로는 S3에 요청 필요)
        //long fileSize = s3Service.getFileSize(fileUrl);

        MetaData metaData = new MetaData();
        metaData.setDocument(document);
        metaData.setType(fileType);
        //metaData.setSize(fileSize);
        metaData.setSize(0L);

        metaDataRepository.save(metaData);
    }

    public String extractExtension(String fileUrl) {
        int lastDot = fileUrl.lastIndexOf(".");
        if (lastDot == -1) return null;
        return fileUrl.substring(lastDot + 1);
    }
}
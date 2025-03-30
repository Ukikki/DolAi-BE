package com.dolai.backend.document.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.document.model.MetaData;
import com.dolai.backend.document.model.MetaDataResponseDto;
import com.dolai.backend.document.repository.MetaDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}

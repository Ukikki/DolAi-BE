package com.dolai.backend.document.service;

import com.dolai.backend.document.model.DocumentPlacement;
import com.dolai.backend.document.repository.DocumentPlacementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentPlacementService {
    private final DocumentPlacementRepository documentPlacementRepository;

    // 문서 위치 매핑 저장
    public DocumentPlacement saveDocumentPlacement(DocumentPlacement documentPlacement) {
        return documentPlacementRepository.save(documentPlacement);
    }

    // 문서 위치 매핑 조회 (필요시)
    public DocumentPlacement getDocumentPlacement(Long id) {
        return documentPlacementRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Document Placement not found"));
    }
}

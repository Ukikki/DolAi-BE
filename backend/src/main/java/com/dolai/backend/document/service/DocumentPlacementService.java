package com.dolai.backend.document.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.directory.model.Directory;
import com.dolai.backend.document.model.Document;
import com.dolai.backend.document.model.DocumentPlacement;
import com.dolai.backend.document.model.DocumentSummaryDto;
import com.dolai.backend.document.repository.DocumentPlacementRepository;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentPlacementService {
    private final DocumentPlacementRepository documentPlacementRepository;

    @Transactional
    public List<DocumentSummaryDto> getDocumentsByDirectory(Long directoryId, String userId) {
        // 사용자에게 연결된 문서 조회
        List<DocumentPlacement> placements = documentPlacementRepository
                .findAllByDirectoryIdAndUserId(directoryId, userId);

        return placements.stream()
                .map(this::toSummaryDto)
                .toList();
    }

    // DocumentPlacement → DocumentSummaryDto로 변환
    @Transactional
    private DocumentSummaryDto toSummaryDto(DocumentPlacement placement) {
        Document doc = placement.getDocument();
        if (doc == null) throw new CustomException(ErrorCode.DOCUMENT_NOT_FOUND);

        return new DocumentSummaryDto(
                doc.getId(),
                doc.getTitle(),
                doc.getFileType()
        );
    }

    @Transactional
    public void linkDocumentToDirectory(Document document, Directory directory, User user) {
        DocumentPlacement placement = DocumentPlacement.builder()
                .document(document)
                .directory(directory)
                .user(user)
                .build();
        placement.setDocument(document);
        placement.setDirectory(directory);
        placement.setUser(user);
        documentPlacementRepository.save(placement);
    }

}
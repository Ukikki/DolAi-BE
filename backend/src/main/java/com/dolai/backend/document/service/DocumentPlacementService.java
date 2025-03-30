package com.dolai.backend.document.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.directory.model.Directory;
import com.dolai.backend.directory.repository.DirectoryRepository;
import com.dolai.backend.document.model.Document;
import com.dolai.backend.document.model.DocumentPlacement;
import com.dolai.backend.document.model.DocumentSummaryDto;
import com.dolai.backend.document.model.enums.FileType;
import com.dolai.backend.document.repository.DocumentPlacementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentPlacementService {
    private final DocumentPlacementRepository documentPlacementRepository;
    private final DirectoryRepository directoryRepository;

    public List<DocumentSummaryDto> getDocumentsByDirectory(Long directoryId, String userId) {
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DIRECTORY_NOT_FOUND));

        List<DocumentPlacement> placements = documentPlacementRepository
                .findAllByDirectoryIdAndUserId(directoryId, userId);

        return placements.stream()
                .map(placement -> {
                    Document doc = placement.getDocument();
                    if (doc == null) {
                        throw new CustomException(ErrorCode.DOCUMENT_NOT_FOUND);
                    }
                    return new DocumentSummaryDto(
                            doc.getId(),
                            doc.getTitle(),
                            doc.getFileType()
                    );
                })
                .toList();
    }
}
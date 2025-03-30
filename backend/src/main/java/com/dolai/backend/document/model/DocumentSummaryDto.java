package com.dolai.backend.document.model;

import com.dolai.backend.document.model.enums.FileType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class DocumentSummaryDto {
    private Long documentId;
    private String title;
    private FileType fileType;
}

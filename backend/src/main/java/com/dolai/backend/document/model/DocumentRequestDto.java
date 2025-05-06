package com.dolai.backend.document.model;

import com.dolai.backend.document.model.enums.FileType;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequestDto {
    private String fileUrl;
    private String title;
    private FileType fileType;
}
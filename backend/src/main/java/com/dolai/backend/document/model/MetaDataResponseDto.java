package com.dolai.backend.document.model;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MetaDataResponseDto {
    private String type;
    private Long size; // byte 단위
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
}
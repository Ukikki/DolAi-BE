package com.dolai.backend.directory.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DirectoryResponseDto {
    private String status;
    private String message;
    private Long directoryId;
    private String name;
}

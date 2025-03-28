package com.dolai.backend.directory.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DirectoryListResponseDto {
    private String directoryId;
    private String name;
    private String parentDirectoryId;
}

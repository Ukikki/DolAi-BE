package com.dolai.backend.directory.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirectoryRequestDto {
    private String name;
    private Long parentDirectoryId;
    private String type; // "PERSONAL" or "SHARED"
    private String meetingId; // SHARED일 경우 필수
}

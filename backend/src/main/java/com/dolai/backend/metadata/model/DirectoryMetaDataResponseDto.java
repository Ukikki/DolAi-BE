package com.dolai.backend.metadata.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DirectoryMetaDataResponseDto {

    private String type; // SHARED or PERSONAL
    private String size; // 4KB
    private String meetingTitle; // SHARED 타입일 경우만 사용
    private List<ParticipantInfo> participants;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy년 M월 d일 HH:mm", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy년 M월 d일 HH:mm", timezone = "Asia/Seoul")
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ParticipantInfo {
        private String name;
        private String email;
    }
}
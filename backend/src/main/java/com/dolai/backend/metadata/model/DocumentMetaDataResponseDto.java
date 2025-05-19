package com.dolai.backend.metadata.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class DocumentMetaDataResponseDto {
    private String title;            // 문서 제목
    private String type;             // 파일 유형
    private String size;               // 파일 크기
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy년 M월 d일 HH:mm", timezone = "Asia/Seoul")
    private LocalDateTime createdAt; // 생성 시각
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy년 M월 d일 HH:mm", timezone = "Asia/Seoul")
    private LocalDateTime lastModifiedAt; // 수정 시각
    private String summary;          // 문서 요약
}

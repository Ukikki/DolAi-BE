package com.dolai.backend.document.model;

import com.dolai.backend.common.model.BaseTimeEntity;
import com.dolai.backend.document.model.enums.FileType;
import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.meeting.model.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 기본키 (Primary Key)

    @ManyToOne  // Document는 여러 개가 하나의 Meeting과 연결될 수 있음
    @JoinColumn(name = "meeting_id", nullable = false)  // FK 설정
    private Meeting meeting;

    @Column(nullable = false, name = "file_url", length = 2083)
    private String fileUrl; // S3 파일 저장 경로

    @Column(nullable = false)
    private String title;  // 문서 제목

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    public static Document create(Meeting meeting, String fileUrl, String title, FileType fileType) {
        return Document.builder()
                .meeting(meeting)
                .fileUrl(fileUrl)
                .title(title)
                .fileType(fileType)
                .build();
    }
}
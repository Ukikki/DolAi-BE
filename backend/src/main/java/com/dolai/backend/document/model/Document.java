package com.dolai.backend.document.model;

import com.dolai.backend.common.model.BaseTimeEntity;
import com.dolai.backend.meeting.model.Meeting;
import jakarta.persistence.*;
import lombok.*;

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

    // 문서 버전
    @Column(nullable = false)
    private int version;

    @Column(nullable = false, name = "file_url", length = 2083)
    private String fileUrl; // S3 파일 저장 경로

    @Column(nullable = false)
    private String title;  // 문서 제목

    @Column(columnDefinition = "TEXT")
    private String summary;  // 요약 내용

    // private String detailedJsonUrl;
    // private String graphImageUrl;
    // private String notesUrl;
}
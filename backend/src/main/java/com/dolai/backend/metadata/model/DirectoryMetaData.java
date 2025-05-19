package com.dolai.backend.metadata.model;

import com.dolai.backend.common.model.BaseTimeEntity;
import com.dolai.backend.directory.model.Directory;
import com.dolai.backend.meeting.model.Meeting;
import jakarta.persistence.*;
import lombok.*;

/*
타입, 미팅 제목, 참가자(이름과 이메일), 크기
 */
@Entity
@Table(name = "directory_meta_data")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectoryMetaData extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "directory_id", nullable = false)
    private Directory directory;

    @Column(nullable = false)
    private Long size;  // 총 파일 크기

    // 미팅 정보 접근을 위한 편의 메서드
    @Transient
    public Meeting getRelatedMeeting() {
        return directory != null ? directory.getMeeting() : null;
    }

    public static DirectoryMetaData of(Directory directory) {
        return DirectoryMetaData.builder()
                .directory(directory)
                .size(0L)
                .build();
    }
}
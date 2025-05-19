package com.dolai.backend.metadata.model;

import com.dolai.backend.common.model.BaseTimeEntity;
import com.dolai.backend.document.model.Document;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_meta_data")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetaData extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false)
    private String type;  // 파일 유형

    @Column(nullable = false)
    private Long size;  // 파일 크기

    @Column(length = 1000)
    private String summary;  // 문서 요약

    @Column(name = "language", length = 10)
    private String language;  // 문서 언어
}
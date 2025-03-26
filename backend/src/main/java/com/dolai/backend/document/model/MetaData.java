package com.dolai.backend.document.model;

import com.dolai.backend.common.model.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "meta_data")
@Getter
@Setter
public class MetaData extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)  // Document와 다대일 관계
    @JoinColumn(name = "document_id", nullable = false)  // 외래 키 연결
    private Document document;  // 연결된 문서 (Document 엔티티와 관계)

    @Column(nullable = false)
    private String type;  // 파일 유형 (pdf, docx 등)

    @Column(nullable = false)
    private Long size;  // 파일 크기 (Byte 단위)

}

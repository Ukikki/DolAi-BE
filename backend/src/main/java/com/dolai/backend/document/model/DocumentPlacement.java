package com.dolai.backend.document.model;

import com.dolai.backend.directory.model.Directory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "document_placements")
@Getter
@Setter
public class DocumentPlacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "directory_id")
    private Directory directory;  // 폴더 ID (Directory 엔티티와 관계)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;  // 문서 ID (Document 엔티티와 관계)

    @Column(nullable = false)
    private String userId;  // 사용자 고유 ID (User 엔티티와 관계)
}

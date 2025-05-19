package com.dolai.backend.document.model;

import com.dolai.backend.directory.model.Directory;
import com.dolai.backend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_placements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentPlacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "directory_id")
    private Directory directory;  // 폴더 ID (Directory 엔티티와 관계)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;  // 문서 ID (Document 엔티티와 관계)

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

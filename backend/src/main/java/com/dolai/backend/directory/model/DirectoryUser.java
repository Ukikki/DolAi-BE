package com.dolai.backend.directory.model;

import com.dolai.backend.common.model.BaseTimeEntity;
import com.dolai.backend.directory.model.enums.DirectoryColor;
import com.dolai.backend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "directory_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectoryUser extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // directory_id -> Directory 테이블의 id 외래 키
    @ManyToOne
    @JoinColumn(name = "directory_id", referencedColumnName = "id", nullable = false) // directory_id가 directories 테이블의 id를 참조
    private Directory directory;

    // user_id -> User 테이블의 id 외래 키
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false) // user_id가 user 테이블의 id를 참조
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DirectoryColor color = DirectoryColor.BLUE;
}
package com.dolai.backend.directory.model;

import com.dolai.backend.common.model.BaseTimeEntity;
import com.dolai.backend.directory.model.enums.DirectoryType;
import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "directory")
@Getter
@Setter
public class Directory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // userId 외래키 연결
    @ManyToOne
    @JoinColumn(name = "user_id",  nullable = true)  // FK 설정
    private User user;

    // meetingId 외래키 연결
    @ManyToOne
    @JoinColumn(name = "meeting_id", nullable = true)  // FK 설정
    private Meeting meeting;

    // 공동 디렉터리 이름 (미팅 날짜 등)
    @Column
    private String name; // 예: "2025-03-27 회의"

    // 자기참조 관계
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Directory parent;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DirectoryType type;

    @OneToMany(mappedBy = "directory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DirectoryUser> directoryUsers = new ArrayList<>();

}
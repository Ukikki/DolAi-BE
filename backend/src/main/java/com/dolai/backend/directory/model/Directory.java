package com.dolai.backend.directory.model;

import com.dolai.backend.common.model.BaseTimeEntity;
import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
    @JoinColumn(name = "user_id", nullable = false)  // FK 설정
    private User user;

    // meetingId 외래키 연결
    @ManyToOne
    @JoinColumn(name = "meeting_id", nullable = false)  // FK 설정
    private Meeting meeting;

    // 자기참조 관계
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Directory parent;

}
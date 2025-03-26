package com.dolai.backend.meeting.model;

import com.dolai.backend.common.model.BaseTimeEntity;
import com.dolai.backend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User 테이블의 id 외래 키
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    // Meeting 테이블의 id 외래 키
    @ManyToOne
    @JoinColumn(name = "meeting_id", referencedColumnName = "id", nullable = false)
    private Meeting meeting;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.PARTICIPANT;

    public enum Role {
        HOST, PARTICIPANT
    }
}

package com.dolai.backend.todo.model;

import com.dolai.backend.common.model.BaseTimeEntity;
import com.dolai.backend.todo.model.enums.Status;
import com.dolai.backend.user.model.User;
import com.dolai.backend.meeting.model.Meeting;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "todo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Todo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // userId 외래키 연결
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    private LocalDateTime dueDate;  // To-Do 기한

    @ManyToOne
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = Status.PENDING;
        }
    }

    public static Todo create(User user, TodoRequestDto dto, Meeting meeting) {
        return Todo.builder()
                .title(dto.getTitle())
                .dueDate(dto.getDueDate())
                .user(user)
                .meeting(meeting)
                .status(Status.PENDING)
                .build();
    }
}

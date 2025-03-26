package com.dolai.backend.todo.model;

import com.dolai.backend.todo.model.enums.Status;
import com.dolai.backend.user.model.User;
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
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // userId 외래키 연결
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;  // To-Do 내용

    private LocalDateTime dueDate;  // To-Do 기한

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status= Status.PENDING;

}

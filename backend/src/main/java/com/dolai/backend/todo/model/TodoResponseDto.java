package com.dolai.backend.todo.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class TodoResponseDto {
    private Long id;
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime dueDate;

    private String status;

    public static TodoResponseDto from(Todo todo) {
        return TodoResponseDto.builder()
                .id(todo.getId())
                .title(todo.getTitle())
                .dueDate(todo.getDueDate())
                .status(todo.getStatus().name().toLowerCase())  // 예: "PENDING" -> "pending"
                .build();
    }
}
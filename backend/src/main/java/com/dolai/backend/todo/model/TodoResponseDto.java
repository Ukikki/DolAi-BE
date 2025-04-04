package com.dolai.backend.todo.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TodoResponseDto {
    private Long id;
    private String title;
    private String description;
    private String dueDate;
    private String status;
}
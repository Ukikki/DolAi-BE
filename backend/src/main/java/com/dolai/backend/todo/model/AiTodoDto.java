package com.dolai.backend.todo.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiTodoDto {
    private String name;
    private String task;
    private String dueDate;
}
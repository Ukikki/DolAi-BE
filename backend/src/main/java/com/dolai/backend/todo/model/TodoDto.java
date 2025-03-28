package com.dolai.backend.todo.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TodoDto {
    private String title;
    private String assignee; // 사용자 email 또는 이름
    private String dueDate;  // ISO-8601 문자열 (예: 2025-03-15T23:59:59Z)
}
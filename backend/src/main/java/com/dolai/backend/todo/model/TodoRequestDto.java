package com.dolai.backend.todo.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TodoRequestDto {
    private String title;
    private String description;
    private String dueDate; // ISO_LOCAL_DATE_TIME
    private String meetingId;
    private String assignee; // ✅ 요게 없으면 빌더가 에러남
}
package com.dolai.backend.todo.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class TodoRequestDto {
    @NotBlank(message = "할 일 제목은 필수입니다.")
    private String title;

    @NotNull(message = "마감일을 선택해주세요.")
    private LocalDateTime dueDate;

    private String meetingId;

    private String assignee; // AI To-do 생성 시에만 사용
}
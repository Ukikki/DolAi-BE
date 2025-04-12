package com.dolai.backend.todo.model;

import com.dolai.backend.todo.model.enums.Status;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TodoStatusUpdateRequestDto {
    @NotNull(message = "상태 값은 필수입니다.")
    private Status status;
}
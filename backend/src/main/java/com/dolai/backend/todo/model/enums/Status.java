package com.dolai.backend.todo.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Status {
    PENDING, IN_PROGRESS, COMPLETED;

    @JsonCreator
    public static Status from(String value) {
        return Status.valueOf(value.toUpperCase());
    }
}
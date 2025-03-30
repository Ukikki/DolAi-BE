package com.dolai.backend.common.success;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SuccessDataResponse<T> {
    private final String status = "success";
    private final T data;
}
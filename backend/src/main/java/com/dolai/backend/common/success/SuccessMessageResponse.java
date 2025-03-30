package com.dolai.backend.common.success;

import lombok.Getter;

@Getter
public class SuccessMessageResponse {
    private final String status = "success";
    private final String message;

    public SuccessMessageResponse(String message) {
        this.message = message;
    }
}

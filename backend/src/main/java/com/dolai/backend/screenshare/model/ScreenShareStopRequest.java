package com.dolai.backend.screenshare.model;

import lombok.Data;

@Data
public class ScreenShareStopRequest {
    private String userId;
    private String text;
    private String timestamp;
}
package com.dolai.backend.screenshare.model;


import lombok.Data;

@Data
public class ScreenShareRequestDto {
    private String userId;
    private String text;
    private String timestamp;
    // private String imageUrl; // 추후 S3 연동 시 활성화
}

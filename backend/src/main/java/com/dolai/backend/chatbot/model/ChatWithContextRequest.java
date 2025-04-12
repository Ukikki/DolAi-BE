package com.dolai.backend.chatbot.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatWithContextRequest {
    private String meetingId;
    private String message;
}
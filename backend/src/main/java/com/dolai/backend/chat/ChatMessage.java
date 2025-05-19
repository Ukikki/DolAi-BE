package com.dolai.backend.chat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessage {
    private String meetingId;
    private String senderName;
    private String content;
}
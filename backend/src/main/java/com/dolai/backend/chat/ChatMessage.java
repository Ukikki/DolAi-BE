package com.dolai.backend.chat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessage {
    private String meetingId;
    private String senderId;
    private String senderName;
    private String content;
}
package com.dolai.backend.chat;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {
    @MessageMapping("/chat/{meetingId}")
    @SendTo("/topic/chat/{meetingId}")
    public ChatMessage sendMessage(@DestinationVariable String meetingId, ChatMessage message) {
        return message;
    }
}
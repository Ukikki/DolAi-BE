package com.dolai.backend.meeting.model;

import com.dolai.backend.meeting.service.MediasoupWebSocketClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class MeetingResponseDto {
    private String id;
    private String title;
    private LocalDateTime startTime;
    private String inviteUrl;

    // WebSocket에 미팅 생성 요청
    public void sendToMediasoupWebSocket(MediasoupWebSocketClient websocketClient) {
        String message = "{ \"type\": \"CREATE_ROOM\", \"meetingId\": \"" + id + "\" }";
        websocketClient.sendMessage(message);
    }
}

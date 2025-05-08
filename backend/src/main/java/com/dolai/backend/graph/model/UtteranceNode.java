package com.dolai.backend.graph.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UtteranceNode {

    private String id;

    private String meetingId;

    private String speaker;

    private String text;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Long startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Long endTime;

    public UtteranceNode(String utteranceId, String speakerName, String text, Instant timestamp) {
        this.id = utteranceId;
        this.speaker = speakerName;
        this.text = text;
        this.startTime = timestamp.toEpochMilli();
        this.endTime = timestamp.toEpochMilli() + 1000; // 예시로 1초 후로 설정
    }
}
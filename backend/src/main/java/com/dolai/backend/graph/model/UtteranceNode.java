package com.example.demo.graph.model;

import com.arangodb.springframework.annotation.ArangoId;
import com.arangodb.springframework.annotation.Document;
import org.springframework.data.annotation.Id;
import lombok.*;

import java.time.Instant;

@Document("utterances")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class UtteranceNode {

    @Id
    private String id;

    private String meetingId;
    private String speaker;
    private String text;
    private Long startTime;
    private Long endTime;

    public UtteranceNode(String utteranceId, String speakerName, String text, Instant timestamp) {
        this.id = utteranceId;
        this.speaker = speakerName;
        this.text = text;
        this.startTime = timestamp.toEpochMilli();
        this.endTime = timestamp.toEpochMilli() + 1000; // 예시로 1초 후로 설정
    }
}
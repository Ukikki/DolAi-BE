package com.example.demo.graph.model;

import com.arangodb.springframework.annotation.ArangoId;
import com.arangodb.springframework.annotation.Document;
import org.springframework.data.annotation.Id;
import lombok.*;

import java.time.Instant;

@Document("meetings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MeetingNode {

    @Id
    private String id; // == safeArangoKey(meetingId)

    private String meetingId;    // 비즈니스 식별자 (예: "2024-05-01-10AM")
    private String title;        // 회의 제목 (선택)
    private Long timestamp;      // 생성 시간 (epoch)
    private String[] participants; // 참여자 리스트 (선택)

    public MeetingNode(String id, String s, Instant timestamp, Object o) {
        this.id = id;
        this.title = s;
        this.timestamp = timestamp.toEpochMilli();
        this.participants = new String[0]; // 기본값으로 빈 배열
    }
}
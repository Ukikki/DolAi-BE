package com.dolai.backend.graph.entity;


import com.arangodb.springframework.annotation.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import java.time.LocalDateTime;

/**
 * 발화(대화 내용)를 나타내는 Node
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document("utterance_nodes")
public class UtteranceNode {
    @Id
    private String id;
    private String text;
    private String speakerName;
    private LocalDateTime timestamp;
    private String meetingId;  // meeting_nodes/{id}
}


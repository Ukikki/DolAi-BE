package graph.entity;

import com.arangodb.springframework.annotation.Document;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

// 실시간 발화를 나타내는 노드. 화자가 어떤 내용을 발화했는지 표현

@Document("utterances")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtteranceNode {
    @Id
    private String id;
    private String speakerId;
    private String text;
    private LocalDateTime timestamp;
}
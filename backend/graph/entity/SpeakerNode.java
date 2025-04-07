package graph.entity;

import com.arangodb.springframework.annotation.Document;
import lombok.Builder;
import org.springframework.data.annotation.Id;

// 발화자(사용자 또는 화자)를 나타내는 노드
@Document("SpeakerNode")
@Builder
public class SpeakerNode {
    @Id
    private String id;
    private String name;
}

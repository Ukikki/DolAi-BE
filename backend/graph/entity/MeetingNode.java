package graph.entity;

import com.arangodb.springframework.annotation.Document;
import lombok.*;
import org.springframework.data.annotation.Id;
import java.time.LocalDateTime;

/**
 * 화상회의 자체를 그래프의 중심 노드로 표현
 */
@Document("MeetingNode")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingNode {
    @Id
    private String id;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

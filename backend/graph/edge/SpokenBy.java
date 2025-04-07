package graph.edge;

import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;
import graph.entity.SpeakerNode;
import graph.entity.UtteranceNode;
import lombok.*;
import org.springframework.data.annotation.Id;

// 누가 어떤 발화를 했는지 연결

@Edge("SPOKEN_BY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpokenBy {
    @Id
    private String id;

    @From
    private UtteranceNode from;

    @To
    private SpeakerNode to;
}
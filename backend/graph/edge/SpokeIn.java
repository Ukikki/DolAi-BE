package graph.edge;

import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;
import graph.entity.MeetingNode;
import graph.entity.UtteranceNode;
import lombok.*;
import org.springframework.data.annotation.Id;

//  특정 발화가 어떤 회의에서 발생했는지 나타냄
@Edge("SPOKE_IN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpokeIn {
    @Id
    private String id;

    @From
    private UtteranceNode from;

    @To
    private MeetingNode to;
}

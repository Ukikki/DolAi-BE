package graph.edge;


import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;
import graph.entity.MeetingNode;
import graph.entity.SpeakerNode;
import lombok.*;
import org.springframework.data.annotation.Id;

// 사용자가 회의에 참여한 관계 표현
@Document("PARTICIPATED_IN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipatedIn {

    @Id
    private String id;

    @From
    private SpeakerNode from;

    @To
    private MeetingNode to;
}
package com.dolai.backend.graph.edge;

import com.arangodb.springframework.annotation.Edge;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Persistent;

/**
 * 발화 - 회의 연결 Edge
 */
@Edge("utterance_to_meeting_edges")
public class UtteranceToMeetingEdge {
    @Id
    private String id;

    @Persistent
    private String _from;  // utterance_nodes/{utteranceId}

    @Persistent
    private String _to;    // meeting_nodes/{meetingId}
}
package com.dolai.backend.graph.edge;

import com.arangodb.springframework.annotation.Edge;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Persistent;

/**
 * 참가자 - 회의 연결 Edge
 */
@Edge("participant_to_meeting_edges")
public class ParticipantToMeetingEdge {
    @Id
    private String id;
    @Persistent
    private String _from;
    @Persistent
    private String _to;
}
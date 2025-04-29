package com.dolai.backend.graph.edge;

import com.arangodb.springframework.annotation.Edge;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Persistent;

/**
 * 할 일(Task) - 담당자(Participant) 연결 Edge
 */
@Edge("task_to_participant_edges")
public class TaskToParticipantEdge {
    @Id
    private String id;
    @Persistent
    private String _from;
    @Persistent
    private String _to;
}
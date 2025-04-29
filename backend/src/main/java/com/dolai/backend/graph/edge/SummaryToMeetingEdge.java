package com.dolai.backend.graph.edge;

import com.arangodb.springframework.annotation.Edge;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Persistent;

/**
 * 요약(Summary) - 회의 연결 Edge
 */
@Edge("summary_to_meeting_edges")
public class SummaryToMeetingEdge {
    @Id
    private String id;
    @Persistent
    private String _from;
    @Persistent
    private String _to;
}
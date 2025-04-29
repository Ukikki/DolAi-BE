package com.dolai.backend.graph.edge;

import com.arangodb.springframework.annotation.Edge;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Persistent;

/**
 * 발화 - 키워드 연결 Edge
 */
@Edge("utterance_to_keyword_edges")
public class UtteranceToKeywordEdge {
    @Id
    private String id;
    @Persistent
    private String _from;
    @Persistent
    private String _to;
}
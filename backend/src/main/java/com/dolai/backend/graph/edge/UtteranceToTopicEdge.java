package com.dolai.backend.graph.edge;

import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;
import com.dolai.backend.graph.entity.UtteranceNode;
import com.dolai.backend.graph.entity.TopicNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Edge("utteranceToTopicEdges")
@Data
@NoArgsConstructor
public class UtteranceToTopicEdge {

    @Id
    private String id;

    @From
    private UtteranceNode from;

    @To
    private TopicNode to;

    private Instant timestamp;
}

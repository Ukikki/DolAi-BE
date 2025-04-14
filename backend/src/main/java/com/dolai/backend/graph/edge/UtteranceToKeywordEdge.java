package com.dolai.backend.graph.edge;

import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;
import com.dolai.backend.graph.entity.UtteranceNode;
import com.dolai.backend.graph.entity.KeywordNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Edge("utteranceToKeywordEdges")
@Data
@NoArgsConstructor
public class UtteranceToKeywordEdge {

    @Id
    private String id;

    @From
    private UtteranceNode from;

    @To
    private KeywordNode to;

    private Instant timestamp;
}

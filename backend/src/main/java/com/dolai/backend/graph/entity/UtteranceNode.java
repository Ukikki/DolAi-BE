package com.dolai.backend.graph.entity;

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Relations;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.util.Collection;

@Document("utterancenodes")
@Data
@NoArgsConstructor
public class UtteranceNode {

    @Id
    private String id;

    private String text;

    @Relations(edges = com.dolai.backend.graph.edge.UtteranceToTopicEdge.class, lazy = true)
    private Collection<TopicNode> topics;

    @Relations(edges = com.dolai.backend.graph.edge.UtteranceToKeywordEdge.class, lazy = true)
    private Collection<KeywordNode> keywords;
}

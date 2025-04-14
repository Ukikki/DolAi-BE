package com.dolai.backend.graph.entity;

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Relations;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.util.Collection;

@Document("utterancenodes")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtteranceNode {

    @Id
    private String id;

    private String text;

    private String meetingId;

    private String speakerName;

    @Relations(edges = com.dolai.backend.graph.edge.UtteranceToTopicEdge.class, lazy = true)
    private Collection<TopicNode> topics;

    @Relations(edges = com.dolai.backend.graph.edge.UtteranceToKeywordEdge.class, lazy = true)
    private Collection<KeywordNode> keywords;
}

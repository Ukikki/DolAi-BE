package com.dolai.backend.graph.entity;

import com.arangodb.springframework.annotation.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

/**
 * 회의 주제(Topic) Node
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document("topic_nodes")
public class TopicNode {
    @Id
    private String id;
    private String name;
    private String description;
}
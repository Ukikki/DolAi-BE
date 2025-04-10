package com.dolai.backend.graph.entity;

import com.arangodb.springframework.annotation.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Document("topicnodes")
@Data
@NoArgsConstructor
public class TopicNode {
    @Id
    private String id;
    private String name;

    public TopicNode(String topicName) {
        this.name = topicName;
    }
}

package com.dolai.backend.graph.entity;

import com.arangodb.springframework.annotation.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Document("keywordnodes")
@Data
@NoArgsConstructor
public class KeywordNode {
    @Id
    private String id;
    private String name;

    public KeywordNode(String keywordName) {
        this.name = keywordName;
    }
}

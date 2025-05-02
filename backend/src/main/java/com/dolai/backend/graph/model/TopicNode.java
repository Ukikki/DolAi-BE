package com.example.demo.graph.model;

import com.arangodb.springframework.annotation.ArangoId;
import com.arangodb.springframework.annotation.Document;
import org.springframework.data.annotation.Id;
import lombok.*;

@Document("topics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class TopicNode {

    @Id
    private String id;

    private String name; // 예: "협업", "AI", "일정관리"
}

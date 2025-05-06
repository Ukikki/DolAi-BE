package com.dolai.backend.graph.model;

import com.arangodb.springframework.annotation.Document;
import lombok.*;
import org.springframework.data.annotation.Id;

@Document("topics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class TopicNode {

    @Id
    private String id;

    private String name; // 예: "협업", "AI", "일정관리"
}

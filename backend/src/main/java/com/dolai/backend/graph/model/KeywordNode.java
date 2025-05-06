package com.dolai.backend.graph.model;

import com.arangodb.springframework.annotation.Document;
import lombok.*;
import org.springframework.data.annotation.Id;

@Document("keywords")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class KeywordNode {

    @Id
    private String id; // _key로 쓰임

    private String word; // 키워드 자체
}

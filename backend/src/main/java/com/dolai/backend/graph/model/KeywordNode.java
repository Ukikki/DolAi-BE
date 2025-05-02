package com.example.demo.graph.model;

import com.arangodb.springframework.annotation.ArangoId;
import com.arangodb.springframework.annotation.Document;
import org.springframework.data.annotation.Id;
import lombok.*;

@Document("keywords")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class KeywordNode {

    @Id
    private String id; // _key로 쓰임

    private String word; // 키워드 자체
}

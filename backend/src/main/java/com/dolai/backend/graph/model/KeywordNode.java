package com.dolai.backend.graph.model;

import lombok.*;
import org.springframework.data.annotation.Id;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class KeywordNode {

    private String id; // _key로 쓰임

    private String word; // 키워드 자체
}

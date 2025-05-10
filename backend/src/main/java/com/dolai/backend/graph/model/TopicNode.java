package com.dolai.backend.graph.model;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class TopicNode {

    private String id;

    private String name; // 예: "협업", "AI", "일정관리"
}

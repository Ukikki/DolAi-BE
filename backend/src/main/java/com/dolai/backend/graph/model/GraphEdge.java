package com.dolai.backend.graph.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter @Setter
public class GraphEdge {
    private String from;  // 출발 노드 id
    private String to;    // 도착 노드 id
    private String type;  // ex) "meeting_to_utterance", "utterance_to_keyword", etc.

    public GraphEdge(String from, String to, String type) {
        this.from = from;
        this.to = to;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphEdge graphEdge = (GraphEdge) o;
        return Objects.equals(from, graphEdge.from) &&
                Objects.equals(to, graphEdge.to) &&
                Objects.equals(type, graphEdge.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, type);
    }
}
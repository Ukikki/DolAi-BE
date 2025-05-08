package com.dolai.backend.graph.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Getter @Setter
public class GraphNode {
    private String id;            // ex) "utterances/12345"
    private String type;          // ex) "utterances", "keywords", "topics", "speakers", "meetings"
    private String label;         // ex) 발화 텍스트 또는 키워드/토픽 이름
    private List<String> keywords; // only for utterances
    private List<String> topics;   // only for utterances

    public GraphNode(String id, String type, String label,
                     List<String> keywords, List<String> topics) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.keywords = keywords;
        this.topics = topics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return Objects.equals(id, graphNode.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
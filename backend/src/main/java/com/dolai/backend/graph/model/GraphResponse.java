package com.dolai.backend.graph.model;

import lombok.*;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphResponse {
    private String status;
    private String message;
    private String meetingId;
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;
}
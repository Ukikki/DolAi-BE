package com.dolai.backend.graph.controller;

import com.dolai.backend.graph.model.GraphResponse;
import com.dolai.backend.graph.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    @GetMapping("/{meetingId}")
    public Map<String, Object> getGraph(@PathVariable String meetingId) {
        GraphResponse graph = graphService.getGraphVisualization(meetingId);
        return Map.of(
                "status", graph.getStatus(),
                "message", graph.getMessage(),
                "meetingId", graph.getMeetingId(),
                "nodes", graph.getNodes(),
                "edges", graph.getEdges()
        );
    }

    @PostMapping("/sync/{meetingId}")
    public Map<String, Object> syncAndGetGraph(@PathVariable String meetingId) {
        graphService.syncGraphFromMysql(meetingId);
        GraphResponse graph = graphService.getGraphVisualization(meetingId);
        return Map.of(
                "status", graph.getStatus(),
                "message", graph.getMessage(),
                "meetingId", graph.getMeetingId(),
                "nodes", graph.getNodes(),
                "edges", graph.getEdges()
        );
    }
}

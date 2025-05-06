package com.dolai.backend.graph.controller;

import com.dolai.backend.graph.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    // Graph를 MySQL에서 가져와서 ArangoDB에 저장
    @PostMapping("/sync/{meetingId}")
    public ResponseEntity<String> syncGraph(@PathVariable String meetingId) {
        if (meetingId == null || meetingId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("meetingId는 필수입니다.");
        }

        try {
            UUID.fromString(meetingId); // UUID 형식 검증
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("meetingId는 UUID 형식이어야 합니다.");
        }

        graphService.syncGraphFromMysql(meetingId);
        return ResponseEntity.ok("해당 미팅 그래프 동기화 완료: " + meetingId);
    }

    // 프론트에서 렌더링 요청 시 호출할 API
    @GetMapping("/{meetingId}")
    public ResponseEntity<Map<String, Object>> getGraphVisualization(@PathVariable String meetingId) {
        if (meetingId == null || meetingId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "meetingId는 필수입니다."
            ));
        }

        try {
            Map<String, Object> graph = graphService.getGraphVisualization(meetingId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Graph data retrieved successfully",
                    "graph", graph
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "그래프 조회 중 오류 발생: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/debug/{meetingId}")
    public Map<String, Object> debugGraph(@PathVariable String meetingId) {
        return graphService.debugGraphByMeetingId(meetingId);
    }
}

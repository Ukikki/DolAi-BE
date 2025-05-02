package com.example.demo.graph.controller;

import com.example.demo.graph.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    // Graph를 MySQL에서 가져와서 ArangoDB에 저장
    @PostMapping("/sync/{meetingId}")
    public ResponseEntity<String> syncGraph(@PathVariable String meetingId) {
        if (meetingId == null || meetingId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("❌ meetingId는 필수입니다.");
        }
        graphService.syncGraphFromMysql(meetingId);
        return ResponseEntity.ok("Graph sync completed for meeting: " + meetingId);
    }

    @GetMapping("/debug/{meetingId}")
    public Map<String, Object> debugGraph(@PathVariable String meetingId) {
        return graphService.debugGraphByMeetingId(meetingId);
    }
}

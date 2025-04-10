package com.dolai.backend.graph.controller;

import com.dolai.backend.graph.service.GraphService;
import com.dolai.backend.stt_log.model.STTLog;
import com.dolai.backend.stt_log.repository.STTLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/graph")
@RequiredArgsConstructor
public class GraphRAGController {

    private final GraphService graphService;
    private final STTLogRepository sttLogRepository;

    @GetMapping("/rag")
    public ResponseEntity<?> getGraphByMeetingId(@RequestParam("meetingId") String meetingId) {
        List<Map<String, Object>> graphData = graphService.findGraphByMeetingId(meetingId);
        return ResponseEntity.ok(graphData);
    }

    @GetMapping("/topic-graph")
    public ResponseEntity<?> getTopicGraphByMeetingId(@RequestParam("meetingId") String meetingId) {
        Map<String, Object> topicGraph = graphService.findTopicGraphByMeetingId(meetingId);
        return ResponseEntity.ok(topicGraph);
    }

    @GetMapping("/debug")
    public ResponseEntity<?> debugArangoDBStatus(@RequestParam("meetingId") String meetingId) {
        Map<String, Object> status = graphService.debugArangoDBStatus(meetingId);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/regenerate")
    public ResponseEntity<?> regenerateGraphForMeeting(@RequestParam("meetingId") String meetingId) {
        graphService.regenerateGraphFromSTTLogs(meetingId);
        return ResponseEntity.ok(Map.of("status", "Graph regeneration complete for meeting: " + meetingId));
    }
}
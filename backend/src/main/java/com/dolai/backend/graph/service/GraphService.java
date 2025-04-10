package com.dolai.backend.graph.service;

import com.arangodb.ArangoCursor;
import com.arangodb.springframework.core.ArangoOperations;
import com.dolai.backend.graph.edge.UtteranceToKeywordEdge;
import com.dolai.backend.graph.edge.UtteranceToTopicEdge;
import com.dolai.backend.graph.entity.KeywordNode;
import com.dolai.backend.graph.entity.TopicNode;
import com.dolai.backend.graph.entity.UtteranceNode;
import com.dolai.backend.graph.repository.*;
import com.dolai.backend.nlp.service.KeywordExtractionService;
import com.dolai.backend.nlp.service.TopicExtractionService;
import com.dolai.backend.stt_log.model.STTLog;
import com.dolai.backend.stt_log.repository.STTLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphService {

    private final UtteranceNodeRepository utteranceRepo;
    private final TopicNodeRepository topicRepo;
    private final KeywordNodeRepository keywordRepo;
    private final UtteranceToTopicEdgeRepository topicEdgeRepo;
    private final UtteranceToKeywordEdgeRepository keywordEdgeRepo;
    private final STTLogRepository sttLogRepository;
    private final ArangoOperations arangoTemplate;

    private final KeywordExtractionService keywordExtractionService;
    private final TopicExtractionService topicExtractionService;

    @Transactional
    public void linkUtteranceToTopic(UtteranceNode utterance, String topicName) {
        if (topicName == null || topicName.trim().isEmpty()) {
            return;
        }

        log.info("Linking utterance to topic: {} -> {}", utterance.getText(), topicName);

        TopicNode topic = topicRepo.findByName(topicName)
                .orElseGet(() -> {
                    log.info("Creating new topic node: {}", topicName);
                    return topicRepo.save(new TopicNode(topicName));
                });

        // Check if edge already exists
        boolean edgeExists = false;
        try {
            edgeExists = topicEdgeRepo.existsByFromIdAndToId(utterance.getId(), topic.getId());
        } catch (Exception e) {
            log.error("Error checking for existing edge: {}", e.getMessage());
        }

        if (edgeExists) {
            log.info("Edge already exists between utterance and topic");
            return;
        }

        try {
            UtteranceToTopicEdge edge = new UtteranceToTopicEdge();
            edge.setFrom(utterance);
            edge.setTo(topic);
            edge.setTimestamp(Instant.now());

            topicEdgeRepo.save(edge);
            log.info("Created edge between utterance '{}' and topic '{}'", utterance.getText(), topicName);
        } catch (Exception e) {
            log.error("Failed to create edge: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void linkUtteranceToKeyword(UtteranceNode utterance, String keywordText) {
        if (keywordText == null || keywordText.trim().isEmpty()) {
            return;
        }

        log.info("Linking utterance to keyword: {} -> {}", utterance.getText(), keywordText);

        KeywordNode keyword = keywordRepo.findByName(keywordText)
                .orElseGet(() -> {
                    log.info("Creating new keyword node: {}", keywordText);
                    return keywordRepo.save(new KeywordNode(keywordText));
                });

        // Check if edge already exists
        boolean edgeExists = false;
        try {
            edgeExists = keywordEdgeRepo.existsByFromIdAndToId(utterance.getId(), keyword.getId());
        } catch (Exception e) {
            log.error("Error checking for existing edge: {}", e.getMessage());
        }

        if (edgeExists) {
            log.info("Edge already exists between utterance and keyword");
            return;
        }

        try {
            UtteranceToKeywordEdge edge = new UtteranceToKeywordEdge();
            edge.setFrom(utterance);
            edge.setTo(keyword);
            edge.setTimestamp(Instant.now());

            keywordEdgeRepo.save(edge);
            log.info("Created edge between utterance '{}' and keyword '{}'", utterance.getText(), keywordText);
        } catch (Exception e) {
            log.error("Failed to create edge: {}", e.getMessage(), e);
        }
    }

    // Save utterance and automatically create graph connections
    @Transactional
    public void saveUtterance(String meetingId, String speakerName, String text) {
        try {
            log.info("Saving utterance: [{}] {} - {}", meetingId, speakerName, text);

            // Check for existing utterance
            UtteranceNode utterance;
            Optional<UtteranceNode> existingUtterance = Optional.empty();

            try {
                existingUtterance = utteranceRepo.findByMeetingIdAndSpeakerNameAndText(
                        meetingId, speakerName, text);
            } catch (Exception e) {
                log.warn("Error finding existing utterance: {}", e.getMessage());
            }

            if (existingUtterance.isPresent()) {
                utterance = existingUtterance.get();
                log.info("Found existing utterance: {}", utterance.getId());
            } else {
                // Create new utterance
                utterance = UtteranceNode.builder()
                        .meetingId(meetingId)
                        .speakerName(speakerName)
                        .text(text)
                        .build();
                utterance = utteranceRepo.save(utterance);
                log.info("Created new utterance node: {}", utterance.getId());
            }

            // Extract keywords and topics from text
            Set<String> keywords = keywordExtractionService.extractKeywords(text);
            Set<String> topics = topicExtractionService.extractTopics(text);

            log.info("Extracted keywords: {}", keywords);
            log.info("Extracted topics: {}", topics);

            // Create graph connections
            for (String keyword : keywords) {
                linkUtteranceToKeyword(utterance, keyword);
            }

            for (String topic : topics) {
                linkUtteranceToTopic(utterance, topic);
            }

            log.info("Successfully processed utterance and created graph connections");
        } catch (Exception e) {
            log.error("Failed to save utterance to graph database: {}", e.getMessage(), e);
        }
    }

    // graphRAG: Get graph for a specific meeting
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findGraphByMeetingId(String meetingId) {
        log.info("Finding graph data for meeting: {}", meetingId);

        String aql = """
            FOR u IN utterancenodes
              FILTER u.meetingId == @meetingId
              LET topics = (
                FOR t, e IN OUTBOUND u utteranceToTopicEdges
                  RETURN {
                    name: t.name,
                    id: t._id,
                    timestamp: e.timestamp
                  }
              )
              LET keywords = (
                FOR k, e IN OUTBOUND u utteranceToKeywordEdges
                  RETURN {
                    name: k.name,
                    id: k._id,
                    timestamp: e.timestamp
                  }
              )
              RETURN {
                utterance: {
                  id: u._id,
                  text: u.text,
                  speakerName: u.speakerName,
                  timestamp: u.timestamp
                },
                topics: topics,
                keywords: keywords
              }
        """;

        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("meetingId", meetingId);

        try {
//            ArangoCursor<Map<String, Object>> cursor = arangoTemplate.query(aql, bindVars, Map.class);
            // 캐스팅 적용
            ArangoCursor<Map<String, Object>> cursor = (ArangoCursor<Map<String, Object>>) (ArangoCursor<?>) arangoTemplate.query(aql, bindVars, Map.class);
            List<Map<String, Object>> results = cursor.asListRemaining();
            log.info("Found {} graph results for meeting {}", results.size(), meetingId);
            return results;
        } catch (Exception e) {
            log.error("Error finding graph data: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // Get topic graph - all connections between topics in a meeting
    @SuppressWarnings("unchecked")
    public Map<String, Object> findTopicGraphByMeetingId(String meetingId) {
        log.info("Finding topic graph for meeting: {}", meetingId);

        String aql = """
            LET utterances = (
              FOR u IN utterancenodes
                FILTER u.meetingId == @meetingId
                RETURN u
            )
            
            LET nodes = (
              FOR u IN utterances
                FOR t IN OUTBOUND u utteranceToTopicEdges
                  COLLECT topic = t
                  RETURN {
                    id: topic._id,
                    name: topic.name,
                    type: 'topic'
                  }
            )
            
            LET edges = (
              FOR u IN utterances
                FOR t1 IN OUTBOUND u utteranceToTopicEdges
                  FOR t2 IN OUTBOUND u utteranceToTopicEdges
                    FILTER t1 != t2
                    COLLECT FROM = t1, TO = t2 INTO connections
                    RETURN {
                      from: FROM._id,
                      to: TO._id,
                      weight: LENGTH(connections)
                    }
            )
            
            RETURN {
              nodes: nodes,
              edges: edges
            }
        """;

        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("meetingId", meetingId);

        try {
//            ArangoCursor<Map<String, Object>> cursor = arangoTemplate.query(aql, bindVars, Map.class);
            // 캐스팅 적용
            ArangoCursor<Map<String, Object>> cursor = (ArangoCursor<Map<String, Object>>) (ArangoCursor<?>) arangoTemplate.query(aql, bindVars, Map.class);
            List<Map<String, Object>> results = cursor.asListRemaining();

            if (results.isEmpty()) {
                log.warn("No topic graph results found for meeting {}", meetingId);
                return Map.of("nodes", Collections.emptyList(), "edges", Collections.emptyList());
            }

            log.info("Found topic graph for meeting {}", meetingId);
            return results.get(0);
        } catch (Exception e) {
            log.error("Error finding topic graph: {}", e.getMessage(), e);
            return Map.of("nodes", Collections.emptyList(), "edges", Collections.emptyList());
        }
    }

    // Debug method to check if data exists in ArangoDB
    public Map<String, Object> debugArangoDBStatus(String meetingId) {
        Map<String, Object> status = new HashMap<>();

        try {
            // Count utterances
            String countUtterances = "RETURN LENGTH(FOR u IN utterancenodes FILTER u.meetingId == @meetingId RETURN u)";
            Map<String, Object> params = Map.of("meetingId", meetingId);
            Long utteranceCount = arangoTemplate.query(countUtterances, params, Long.class).asListRemaining().getFirst();

            // Count topics
            String countTopics = "RETURN LENGTH(FOR t IN topicnodes RETURN t)";
            Long topicCount = arangoTemplate.query(countTopics, Collections.emptyMap(), Long.class).asListRemaining().getFirst();

            // Count keywords
            String countKeywords = "RETURN LENGTH(FOR k IN keywordnodes RETURN k)";
            Long keywordCount = arangoTemplate.query(countKeywords, Collections.emptyMap(), Long.class).asListRemaining().getFirst();

            // Count topic edges
            String countTopicEdges = """
                RETURN LENGTH(
                  FOR u IN utterancenodes
                    FILTER u.meetingId == @meetingId
                    FOR e IN utteranceToTopicEdges
                      FILTER e._from == u._id
                      RETURN e
                )
            """;
            Long topicEdgeCount = arangoTemplate.query(countTopicEdges, params, Long.class).asListRemaining().getFirst();

            // Count keyword edges
            String countKeywordEdges = """
                RETURN LENGTH(
                  FOR u IN utterancenodes
                    FILTER u.meetingId == @meetingId
                    FOR e IN utteranceToKeywordEdges
                      FILTER e._from == u._id
                      RETURN e
                )
            """;
            Long keywordEdgeCount = arangoTemplate.query(countKeywordEdges, params, Long.class).asListRemaining().getFirst();

            status.put("utteranceCount", utteranceCount);
            status.put("topicCount", topicCount);
            status.put("keywordCount", keywordCount);
            status.put("topicEdgeCount", topicEdgeCount);
            status.put("keywordEdgeCount", keywordEdgeCount);
            status.put("status", "success");

        } catch (Exception e) {
            status.put("status", "error");
            status.put("message", e.getMessage());
        }

        return status;
    }

    public void regenerateGraphFromSTTLogs(String meetingId) {
        List<STTLog> logs = sttLogRepository.findByMeetingId(meetingId);

        for (STTLog sttLog : logs) {
            saveUtterance(
                    sttLog.getMeeting().getId().toString(),
                    sttLog.getSpeakerName(),
                    sttLog.getText()
            );
        }

        log.info("✅ Finished regenerating graph for meeting {}", meetingId);
    }

}
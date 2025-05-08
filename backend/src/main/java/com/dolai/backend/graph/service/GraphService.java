package com.dolai.backend.graph.service;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.BaseDocument;
import com.dolai.backend.graph.edge.*;
import com.dolai.backend.graph.model.*;
import com.dolai.backend.nlp.service.KeywordExtractionService;
import com.dolai.backend.nlp.service.TopicExtractionService;
import com.dolai.backend.stt_log.model.STTLog;
import com.dolai.backend.stt_log.repository.STTLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
@Service
public class GraphService {

    private final STTLogRepository sttLogRepository;
    private final KeywordExtractionService keywordExtractor;
    private final TopicExtractionService topicExtractor;
    private final KeywordService keywordService;
    private final TopicService topicService;

    private final ArangoDatabase arangoDatabase;

    public void syncGraphFromMysql(String meetingId) {
        List<STTLog> logs = sttLogRepository.findByMeetingIdOrderByTimestampAsc(meetingId);
        for (STTLog log : logs) {
            saveUtterance(
                    log.getMeeting().getId(),
                    log.getSpeakerName(),
                    log.getText(),
                    log.getTimestamp().atZone(ZoneId.systemDefault()).toInstant()
            );
        }
    }

    public void saveUtterance(String meetingId, String speakerName, String text, Instant timestamp) {
        String utteranceId = UUID.randomUUID().toString();
        UtteranceNode utterance = new UtteranceNode(utteranceId, speakerName, text, timestamp);
        utterance.setMeetingId(meetingId);

        String utteranceArangoId = arangoDatabase.collection("utterances")
                .insertDocument(utterance).getId();

        String speakerKey = safeArangoKey(speakerName);
        String speakerArangoId = "speakers/" + speakerKey;
        SpeakerNode speaker = new SpeakerNode(speakerKey, speakerName);
        if (!arangoDatabase.collection("speakers").documentExists(speakerKey)) {
            arangoDatabase.collection("speakers").insertDocument(speaker);
        }
        arangoDatabase.collection("utterance_to_speaker")
                .insertDocument(new UtteranceToSpeakerEdge(utteranceArangoId, speakerArangoId));

        String meetingArangoId = "meetings/" + meetingId;
        MeetingNode meeting = new MeetingNode(meetingId, "Meeting " + meetingId, timestamp, null);
        if (!arangoDatabase.collection("meetings").documentExists(meetingId)) {
            arangoDatabase.collection("meetings").insertDocument(meeting);
        }
        arangoDatabase.collection("meeting_to_utterance")
                .insertDocument(new MeetingToUtteranceEdge(meetingArangoId, utteranceArangoId));

        for (String keyword : keywordExtractor.extract(text)) {
            String keywordId = safeArangoKey(keyword);
            String keywordArangoId = "keywords/" + keywordId;
            KeywordNode keywordNode = new KeywordNode(keywordId, keyword);
            if (!arangoDatabase.collection("keywords").documentExists(keywordId)) {
                arangoDatabase.collection("keywords").insertDocument(keywordNode);
            }
            arangoDatabase.collection("utterance_to_keyword")
                    .insertDocument(new UtteranceToKeywordEdge(utteranceArangoId, keywordArangoId));
        }

        for (String topic : topicExtractor.extract(text)) {
            String topicId = safeArangoKey(topic);
            String topicArangoId = "topics/" + topicId;
            TopicNode topicNode = new TopicNode(topicId, topic);
            if (!arangoDatabase.collection("topics").documentExists(topicId)) {
                arangoDatabase.collection("topics").insertDocument(topicNode);
            }
            arangoDatabase.collection("utterance_to_topic")
                    .insertDocument(new UtteranceToTopicEdge(utteranceArangoId, topicArangoId));
        }
    }

    public String safeArangoKey(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public Map<String, Object> debugGraphByMeetingId(String meetingId) {
        String meetingArangoId = "meetings/" + meetingId;
        String aql = """
            FOR v, e, p IN 1..3 OUTBOUND @startVertex GRAPH "dolai"
                RETURN {
                    vertex: v,
                    edge: e
                }
            """;
        Map<String, Object> bindVars = Map.of("startVertex", meetingArangoId);
        ArangoCursor<Map> cursor = arangoDatabase.query(aql, bindVars, null, Map.class);
        List<Map> result = cursor.asListRemaining();

        return Map.of(
                "meetingId", meetingId,
                "startVertex", meetingArangoId,
                "graph", result
        );
    }

    public Mono<List<String>> getContextByMeetingId(String meetingId) {
        List<String> contexts = arangoDatabase.query(
                """
                    FOR u IN utterances
                        FILTER u.meetingId == @meetingId
                        SORT u.timestamp ASC
                        RETURN u.text
                """,
                Map.of("meetingId", meetingId),
                null,
                String.class
        ).asListRemaining();
        return Mono.just(contexts);
    }

    public GraphResponse getGraphVisualization(String meetingId) {
        GraphNode meetingNode = new GraphNode("meetings/" + meetingId, "meetings", meetingId, null, null);
        List<UtteranceNode> utterances = new ArrayList<>();

        String query = "FOR u IN utterances FILTER u.meetingId == @meetingId RETURN u";
        Map<String, Object> bindVars = Map.of("meetingId", meetingId);
        ArangoCursor<UtteranceNode> cursor = arangoDatabase.query(query, bindVars, null, UtteranceNode.class);
        cursor.forEachRemaining(utterances::add);

        List<GraphNode> nodes = new ArrayList<>(List.of(meetingNode));
        List<GraphEdge> edges = new ArrayList<>();

        for (UtteranceNode utterance : utterances) {
            String utteranceId = "utterances/" + utterance.getId();
            List<String> keywords = keywordService.extractAndSaveKeywords(utterance.getText());
            List<String> topics = topicService.extractAndSaveTopics(Collections.singletonList(utterance))
                    .getOrDefault(utterance.getId(), Collections.emptyList());

            nodes.add(new GraphNode(utteranceId, "utterances", utterance.getText(), keywords, topics));
            keywords.forEach(keyword -> {
                String keywordId = "keywords/" + keyword;
                nodes.add(new GraphNode(keywordId, "keywords", keyword, null, null));
                edges.add(new GraphEdge(utteranceId, keywordId, "utterance_to_keyword"));
            });
            topics.forEach(topic -> {
                String topicId = "topics/" + topic;
                nodes.add(new GraphNode(topicId, "topics", topic, null, null));
                edges.add(new GraphEdge(utteranceId, topicId, "utterance_to_topic"));
            });
            if (utterance.getSpeaker() != null) {
                String speakerId = safeArangoKey(utterance.getSpeaker());
                String speakerNodeId = "speakers/" + speakerId;
                nodes.add(new GraphNode(speakerNodeId, "speakers", utterance.getSpeaker(), null, null));
                edges.add(new GraphEdge(utteranceId, speakerNodeId, "utterance_to_speaker"));
            }
            edges.add(new GraphEdge(meetingNode.getId(), utteranceId, "meeting_to_utterance"));
        }

        return new GraphResponse("success", "Graph data retrieved successfully", meetingId, nodes, edges);
    }

    public GraphResponse syncAndGetGraph(String meetingId) {
        syncGraphFromMysql(meetingId);
        return getGraphVisualization(meetingId);
    }

    public List<BaseDocument> getUtterances(String meetingId) {
        String query = "FOR u IN utterances FILTER u.meetingId == @meetingId RETURN u";
        Map<String, Object> bindVars = Map.of("meetingId", meetingId);
        ArangoCursor<BaseDocument> cursor = arangoDatabase.query(query, bindVars, null, BaseDocument.class);
        return StreamSupport.stream(cursor.spliterator(), false).collect(Collectors.toList());
    }
}
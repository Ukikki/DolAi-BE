package com.example.demo.graph.service;

import com.arangodb.ArangoDatabase;
import com.example.demo.graph.edge.*;
import com.example.demo.graph.model.*;
import com.example.demo.nlp.service.KeywordExtractionService;
import com.example.demo.nlp.service.TopicExtractionService;
import com.example.demo.stt_log.model.STTLog;
import com.example.demo.stt_log.repository.STTLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GraphService {

    private final STTLogRepository sttLogRepository;
    private final ArangoDatabase arangoDatabase;
    private final KeywordExtractionService keywordExtractor;
    private final TopicExtractionService topicExtractor;

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
        // UtteranceNode
        String utteranceId = UUID.randomUUID().toString();
        UtteranceNode utterance = new UtteranceNode(utteranceId, speakerName, text, timestamp);
        utterance.setMeetingId(meetingId);

        // 문서 삽입 후, ArangoDB가 반환하는 "_id" 값을 통해 안전하게 edge 연결에 사용
        var inserted = arangoDatabase.collection("utterances").insertDocument(utterance);

        // 주의: 직접 "utterances/" + id로 조합하지 않고,
        // ArangoDB가 실제로 저장한 정확한 _id 값을 사용해야
        // 나중에 edge에서 _from, _to 로 연결 시 vertex를 정확히 찾을 수 있음

        // 로그
        System.out.println("Utterance inserted: " + utterance.getId());  // 실제 UUID key
        System.out.println("Utterance _id: utterances/" + utterance.getId());

        String utteranceArangoId = inserted.getId();

        // SpeakerNode
        String speakerKey = safeArangoKey(speakerName);
        String speakerArangoId = "speakers/" + speakerKey;
        SpeakerNode speaker = new SpeakerNode(speakerKey, speakerName);
        if (!arangoDatabase.collection("speakers").documentExists(speakerKey)) {
            arangoDatabase.collection("speakers").insertDocument(speaker);
        }

        // 로그
        System.out.println("Creating edge utterance_to_speaker:");
        System.out.println("  _from: " + utteranceArangoId);
        System.out.println("  _to: " + speakerArangoId);

        arangoDatabase.collection("utterance_to_speaker")
                .insertDocument(new UtteranceToSpeakerEdge(utteranceArangoId, speakerArangoId));

        // MeetingNode
        String meetingArangoId = "meetings/" + meetingId;
        MeetingNode meeting = new MeetingNode(meetingId, "Meeting " + meetingId, timestamp, null);
        if (!arangoDatabase.collection("meetings").documentExists(meetingId)) {
            arangoDatabase.collection("meetings").insertDocument(meeting);
        }
        arangoDatabase.collection("meeting_to_utterance")
                .insertDocument(new MeetingToUtteranceEdge(meetingArangoId, utteranceArangoId));

        // KeywordNodes
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

        // TopicNodes
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

    // ArangoDB에서 사용할 수 있는 안전한 키 생성
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

    // 디버그용 처리 뷰
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

        var cursor = arangoDatabase.query(aql, bindVars, null, Map.class);
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

}

package com.dolai.backend.graph.service;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.BaseDocument;
import com.dolai.backend.graph.edge.*;
import com.dolai.backend.graph.model.*;
import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.meeting.repository.MeetingRepository;
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

    private final MeetingRepository meetingRepository;

    private final ArangoDatabase arangoDatabase;

    // MySQL -> ArangoDB 데이터 동기화 로직
    public void syncGraphFromMysql(String meetingId) {
        List<STTLog> logs = sttLogRepository.findByMeetingIdOrderByTimestampAsc(meetingId);
        for (STTLog log : logs) {
            saveUtterance(
                    log.getMeeting().getId(),
                    log.getSpeakerName(),
                    log.getTextKo(), // getText_ko()로 한국어 문장만 읽어오기
                    log.getTimestamp().atZone(ZoneId.systemDefault()).toInstant()
            );
        }
    }

    public void saveUtterance(String meetingId, String speakerName, String text, Instant timestamp) {
        String utteranceId = UUID.randomUUID().toString();

        // 추출 먼저 수행
        List<String> keywords = keywordExtractor.extract(text);
        List<String> topics = topicExtractor.extract(text);

        // 추출한 값을 포함하여 utterance 노드 구성
        UtteranceNode utterance = new UtteranceNode(utteranceId, speakerName, text, timestamp);
        utterance.setMeetingId(meetingId);
        utterance.setKeywords(keywords);
        utterance.setTopics(topics);

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

    // ArangoDB로부터 그래프 데이터 조회 (meetingTitle 포함)
    public GraphResponse getGraphVisualization(String meetingId) {
        // 1. 회의 제목을 MySQL에서 조회 (없으면 "Untitled Meeting" 사용)
        Optional<Meeting> meetingOpt = meetingRepository.findById(meetingId);
        String meetingTitle = meetingOpt.map(Meeting::getTitle).orElse("Untitled Meeting");

        // 2. 회의 노드 생성
        GraphNode meetingNode = new GraphNode("meetings/" + meetingId, "meetings", meetingTitle, null, null);
        List<UtteranceNode> utterances = new ArrayList<>();

        // 3. ArangoDB에서 해당 회의의 발화(utterance) 리스트 조회
        String query = "FOR u IN utterances FILTER u.meetingId == @meetingId RETURN u";
        Map<String, Object> bindVars = Map.of("meetingId", meetingId);
        ArangoCursor<UtteranceNode> cursor = arangoDatabase.query(query, bindVars, null, UtteranceNode.class);
        cursor.forEachRemaining(utterances::add);

        // 4. 그래프 노드와 엣지 초기화
        List<GraphNode> nodes = new ArrayList<>(List.of(meetingNode));
        List<GraphEdge> edges = new ArrayList<>();

        // 5. 각 발화 노드 처리
        for (UtteranceNode utterance : utterances) {
            String utteranceId = "utterances/" + utterance.getId();
            List<String> keywords = utterance.getKeywords() != null ? utterance.getKeywords() : List.of();
            List<String> topics = utterance.getTopics() != null ? utterance.getTopics() : List.of();

            // 5-1. 발화 노드 추가
            nodes.add(new GraphNode(utteranceId, "utterances", utterance.getText(), keywords, topics));

            // 5-2. 키워드 노드 및 엣지 추가
            for (String keyword : keywords) {
                String keywordId = "keywords/" + keyword;
                nodes.add(new GraphNode(keywordId, "keywords", keyword, null, null));
                edges.add(new GraphEdge(utteranceId, keywordId, "utterance_to_keyword"));
            }

            // 5-3. 토픽 노드 및 엣지 추가
            for (String topic : topics) {
                String topicId = "topics/" + topic;
                nodes.add(new GraphNode(topicId, "topics", topic, null, null));
                edges.add(new GraphEdge(utteranceId, topicId, "utterance_to_topic"));
            }

            // 5-4. 화자 노드 및 엣지 추가 (화자 정보가 있는 경우)
            if (utterance.getSpeaker() != null) {
                String speakerId = safeArangoKey(utterance.getSpeaker());
                String speakerNodeId = "speakers/" + speakerId;
                nodes.add(new GraphNode(speakerNodeId, "speakers", utterance.getSpeaker(), null, null));
                edges.add(new GraphEdge(utteranceId, speakerNodeId, "utterance_to_speaker"));
            }

            // 5-5. 발화와 회의 간 엣지 추가
            edges.add(new GraphEdge(meetingNode.getId(), utteranceId, "meeting_to_utterance"));
        }

        // 6. 최종 그래프 응답 반환
        return new GraphResponse("success", "Graph data retrieved successfully", meetingTitle, nodes, edges);
    }
}
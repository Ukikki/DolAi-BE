package com.dolai.backend.graph.service;

import com.arangodb.springframework.core.ArangoOperations;
import com.dolai.backend.graph.edge.UtteranceToKeywordEdge;
import com.dolai.backend.graph.edge.UtteranceToTopicEdge;
import com.dolai.backend.graph.entity.KeywordNode;
import com.dolai.backend.graph.entity.TopicNode;
import com.dolai.backend.graph.entity.UtteranceNode;
import com.dolai.backend.graph.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GraphService {

    private final UtteranceNodeRepository utteranceRepo;
    private final TopicNodeRepository topicRepo;
    private final KeywordNodeRepository keywordRepo;
    private final UtteranceToTopicEdgeRepository topicEdgeRepo;
    private final UtteranceToKeywordEdgeRepository keywordEdgeRepo;
    private final UtteranceNodeRepository utteranceNodeRepository;

    public void linkUtteranceToTopic(String utteranceText, String topicName) {
        UtteranceNode utterance = new UtteranceNode();
        utterance.setText(utteranceText);
        utterance = utteranceRepo.save(utterance);

        TopicNode topic = topicRepo.findByName(topicName)
                .orElseGet(() -> topicRepo.save(new TopicNode(topicName)));

        UtteranceToTopicEdge edge = new UtteranceToTopicEdge();
        edge.setFrom(utterance);
        edge.setTo(topic);
        edge.setTimestamp(Instant.now());

        topicEdgeRepo.save(edge);
    }

    public void linkUtteranceToKeyword(String utteranceText, String keywordText) {
        UtteranceNode utterance = new UtteranceNode();
        utterance.setText(utteranceText);
        utterance = utteranceRepo.save(utterance);

        KeywordNode keyword = keywordRepo.findByName(keywordText)
                .orElseGet(() -> keywordRepo.save(new KeywordNode(keywordText)));

        UtteranceToKeywordEdge edge = new UtteranceToKeywordEdge();
        edge.setFrom(utterance);
        edge.setTo(keyword);
        edge.setTimestamp(Instant.now());

        keywordEdgeRepo.save(edge);
    }

    // 저장
    public void saveUtterance(String meetingId, String speakerName, String text) {
        // 중복 체크
        boolean exists = utteranceRepo.existsByMeetingIdAndSpeakerNameAndText(meetingId, speakerName, text);
        if (exists) {
//            log.debug("⏭️ Already exists in ArangoDB: [{} - {} - {}]", meetingId, speakerName, text);
            return;
        }

        // 새로운 Utterance 생성 및 저장
        UtteranceNode node = UtteranceNode.builder()
                .meetingId(meetingId)
                .speakerName(speakerName)
                .text(text)
                .build();

        utteranceRepo.save(node);
    }
}

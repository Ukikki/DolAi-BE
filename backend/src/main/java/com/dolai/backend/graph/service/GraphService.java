package com.dolai.backend.graph.service;

import com.arangodb.springframework.core.ArangoOperations;
import com.dolai.backend.graph.edge.UtteranceToKeywordEdge;
import com.dolai.backend.graph.edge.UtteranceToTopicEdge;
import com.dolai.backend.graph.entity.KeywordNode;
import com.dolai.backend.graph.entity.TopicNode;
import com.dolai.backend.graph.entity.UtteranceNode;
import com.dolai.backend.graph.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.ComponentScan;
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
}

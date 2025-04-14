package com.dolai.backend.graph.runner;

import com.dolai.backend.graph.entity.UtteranceNode;
import com.dolai.backend.graph.entity.TopicNode;
import com.dolai.backend.graph.repository.UtteranceNodeRepository;
import com.dolai.backend.graph.repository.TopicNodeRepository;
import com.arangodb.springframework.core.ArangoOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.util.Optional;

@ComponentScan("com.dolai.backend")
@Component
public class CrudRunner implements CommandLineRunner {

    @Autowired
    private ArangoOperations operations;

    @Autowired
    private UtteranceNodeRepository utteranceRepo;

    @Autowired
    private TopicNodeRepository topicRepo;

    @Override
    public void run(String... args) throws Exception {
        // 데이터베이스 초기화: 기존 데이터 삭제
        operations.dropDatabase();

        // UtteranceNode와 TopicNode 저장
        UtteranceNode utterance = new UtteranceNode();
        utterance.setText("What is the weather like today?");
        utterance = utteranceRepo.save(utterance);

        TopicNode topic = new TopicNode();
        topic.setName("Weather");
        topic = topicRepo.save(topic);

        // 저장된 데이터를 확인
        Optional<UtteranceNode> foundUtterance = utteranceRepo.findById(utterance.getId());
        assert foundUtterance.isPresent();
        System.out.println(String.format("Found Utterance: %s", foundUtterance.get()));

        Optional<TopicNode> foundTopic = topicRepo.findById(topic.getId());
        assert foundTopic.isPresent();
        System.out.println(String.format("Found Topic: %s", foundTopic.get()));

        System.out.println("Utterance and Topic saved successfully!");
    }
}

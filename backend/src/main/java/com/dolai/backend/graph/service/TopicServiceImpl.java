package com.dolai.backend.graph.service;

import com.arangodb.ArangoDatabase;
import com.dolai.backend.graph.model.TopicNode;
import com.dolai.backend.graph.model.UtteranceNode;
import com.dolai.backend.nlp.service.TopicExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    private final ArangoDatabase arangoDatabase;
    private final TopicExtractionService extractor;

    @Override
    public Map<String, List<String>> extractAndSaveTopics(List<UtteranceNode> utterances) {
        Map<String, List<String>> result = new HashMap<>();

        for (UtteranceNode utterance : utterances) {
            List<String> topics = extractor.extract(utterance.getText());
            List<String> savedIds = new ArrayList<>();

            for (String topic : topics) {
                String id = com.dolai.backend.graph.util.ArangoKeyUtil.safeKey(topic);
                String docId = "topics/" + id;
                if (!arangoDatabase.collection("topics").documentExists(id)) {
                    arangoDatabase.collection("topics").insertDocument(new TopicNode(id, topic));
                }
                savedIds.add(id);
            }

            result.put(utterance.getId(), savedIds);
        }

        return result;
    }
}

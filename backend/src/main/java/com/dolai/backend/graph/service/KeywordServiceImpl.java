package com.dolai.backend.graph.service;

import com.dolai.backend.graph.model.KeywordNode;
import com.arangodb.ArangoDatabase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KeywordServiceImpl implements KeywordService {

    private final ArangoDatabase arangoDatabase;
    private final com.dolai.backend.nlp.service.KeywordExtractionService extractor;

    @Override
    public List<String> extractAndSaveKeywords(String text) {
        List<String> keywords = extractor.extract(text);
        List<String> savedIds = new ArrayList<>();

        for (String keyword : keywords) {
            String id = com.dolai.backend.graph.util.ArangoKeyUtil.safeKey(keyword);
            String docId = "keywords/" + id;
            if (!arangoDatabase.collection("keywords").documentExists(id)) {
                arangoDatabase.collection("keywords").insertDocument(new KeywordNode(id, keyword));
            }
            savedIds.add(id);
        }

        return savedIds;
    }
}

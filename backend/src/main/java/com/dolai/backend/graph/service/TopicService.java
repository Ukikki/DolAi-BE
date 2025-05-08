package com.dolai.backend.graph.service;

import com.dolai.backend.graph.model.UtteranceNode;

import java.util.List;
import java.util.Map;

public interface TopicService {
    Map<String, List<String>> extractAndSaveTopics(List<UtteranceNode> utterances);
}
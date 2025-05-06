package com.dolai.backend.nlp.service;

import java.util.List;

public interface TopicExtractionService {
    List<String> extract(String text);
}

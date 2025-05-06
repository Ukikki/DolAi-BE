package com.dolai.backend.nlp.service;

import java.util.List;

public interface KeywordExtractionService {
    List<String> extract(String text);
}


package com.dolai.backend.graph.service;

import java.util.List;

public interface KeywordService {
    List<String> extractAndSaveKeywords(String text);
}

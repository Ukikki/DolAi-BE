
package com.dolai.backend.nlp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TopicExtractionService {

    private final Set<String> commonTopics = new HashSet<>(Arrays.asList(
            "project", "timeline", "deadline", "budget", "meeting", "presentation",
            "report", "planning", "strategy", "development", "design", "implementation",
            "testing", "resources", "team", "client", "customer", "product", "service",
            "market", "sales", "marketing", "finance", "hr", "training", "issue", "problem",
            "solution", "action", "decision", "review", "progress", "status", "update"
    ));

    private final Set<String> stopWordsKorean = new HashSet<>(Arrays.asList(
            "이", "그", "저", "이번", "그럼", "그것", "이것", "이런", "저런"
    ));

    private final List<Pattern> topicPatterns = Arrays.asList(
            Pattern.compile("discuss(?:ing)? ([a-zA-Z가-힣]+)"),
            Pattern.compile("([a-zA-Z가-힣]+) (?:timeline|planning|schedule)"),
            Pattern.compile("([a-zA-Z가-힣]+) (?:budget|cost|expense)"),
            Pattern.compile("([a-zA-Z가-힣]+) (?:report|presentation|update)")
    );

    public Set<String> extractTopics(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptySet();
        }

        log.debug("Extracting topics from text: {}", text);
        Set<String> extractedTopics = new HashSet<>();

        for (String topic : commonTopics) {
            if (text.toLowerCase().contains(topic.toLowerCase())) {
                extractedTopics.add(topic);
                log.debug("Found common topic: {}", topic);
            }
        }

        for (Pattern pattern : topicPatterns) {
            Matcher matcher = pattern.matcher(text.toLowerCase());
            while (matcher.find()) {
                String topic = matcher.group(1);
                if (topic.length() > 2) {
                    extractedTopics.add(topic);
                    log.debug("Found topic through pattern: {}", topic);
                }
            }
        }

        if (extractedTopics.size() > 3) {
            extractedTopics = extractedTopics.stream().limit(3).collect(Collectors.toSet());
        }

        log.debug("Extracted topics: {}", extractedTopics);
        return extractedTopics;
    }
}

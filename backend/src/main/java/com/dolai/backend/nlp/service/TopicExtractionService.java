package com.dolai.backend.nlp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TopicExtractionService {

    // Common topics in business meetings
    private final Set<String> commonTopics = new HashSet<>(Arrays.asList(
            "project", "timeline", "deadline", "budget", "meeting", "presentation",
            "report", "planning", "strategy", "development", "design", "implementation",
            "testing", "resources", "team", "client", "customer", "product", "service",
            "market", "sales", "marketing", "finance", "hr", "training", "issue", "problem",
            "solution", "action", "decision", "review", "progress", "status", "update",
            "데드라인", "예산", "회의", "발표", "보고서", "계획", "전략", "개발", "설계", "구현",
            "테스트", "자원", "팀", "고객", "제품", "서비스", "시장", "판매", "마케팅", "재무",
            "인사", "교육", "문제", "해결책", "조치", "결정", "검토", "진행", "상태", "업데이트"
    ));

    // Topic patterns (e.g., "discuss the X", "X planning", etc.)
    private final List<Pattern> topicPatterns = Arrays.asList(
            Pattern.compile("discuss(?:ing)? (?:the|our) ([a-zA-Z가-힣]+)"),
            Pattern.compile("([a-zA-Z가-힣]+) (?:timeline|planning|schedule)"),
            Pattern.compile("([a-zA-Z가-힣]+) (?:budget|cost|expense)"),
            Pattern.compile("([a-zA-Z가-힣]+) (?:report|presentation|update)"),
            Pattern.compile("([a-zA-Z가-힣]+) (?:issue|problem|challenge)")
    );

    // Topics specifically for Korean language
    private final List<Pattern> koreanPatterns = Arrays.asList(
            Pattern.compile("([가-힣]+) (?:계획|일정)"),
            Pattern.compile("([가-힣]+) (?:예산|비용)"),
            Pattern.compile("([가-힣]+) (?:보고서|발표)"),
            Pattern.compile("([가-힣]+) (?:문제|이슈|도전)")
    );

    /**
     * Extracts topics from text
     */
    public Set<String> extractTopics(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptySet();
        }

        log.debug("Extracting topics from text: {}", text);
        Set<String> extractedTopics = new HashSet<>();

        // Look for common topics in the text
        for (String topic : commonTopics) {
            if (text.toLowerCase().contains(topic.toLowerCase())) {
                extractedTopics.add(topic);
                log.debug("Found common topic: {}", topic);
            }
        }

        // Look for topic patterns in English
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

        // Look for topic patterns in Korean
        for (Pattern pattern : koreanPatterns) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String topic = matcher.group(1);
                if (topic.length() > 1) {
                    extractedTopics.add(topic);
                    log.debug("Found Korean topic through pattern: {}", topic);
                }
            }
        }

        // If we didn't find any topics but text is longer than 5 words, extract a topic from the beginning
        if (extractedTopics.isEmpty()) {
            String[] words = text.split("\\s+");
            if (words.length > 5) {
                // Take the first non-stopword as a topic if it's long enough
                for (String word : words) {
                    if (word.length() > 3 && !isStopWord(word)) {
                        extractedTopics.add(word);
                        log.debug("Using fallback topic extraction: {}", word);
                        break;
                    }
                }
            }
        }

        // Ensure we don't return too many topics for a single utterance
        if (extractedTopics.size() > 3) {
            log.debug("Limiting topics from {} to 3", extractedTopics.size());
            return extractedTopics.stream()
                    .limit(3)
                    .collect(java.util.stream.Collectors.toSet());
        }

        log.debug("Extracted topics: {}", extractedTopics);
        return extractedTopics;
    }

    private boolean isStopWord(String word) {
        String lowerWord = word.toLowerCase();
        return lowerWord.equals("the") || lowerWord.equals("a") || lowerWord.equals("an") ||
                lowerWord.equals("and") || lowerWord.equals("or") || lowerWord.equals("but") ||
                lowerWord.equals("in") || lowerWord.equals("on") || lowerWord.equals("at") ||
                lowerWord.equals("to") || lowerWord.equals("for") || lowerWord.equals("with");
    }
}
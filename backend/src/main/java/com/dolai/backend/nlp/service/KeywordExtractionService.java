
package com.dolai.backend.nlp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KeywordExtractionService {
    private final Set<String> stopWords = new HashSet<>(Arrays.asList(
        "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", "be", "have", "has",
        "had", "do", "does", "did", "to", "from", "in", "out", "on", "off", "over", "under",
        "again", "further", "then", "once", "here", "there", "when", "where", "why", "how",
        "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no",
        "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can",
        "will", "just", "don", "should", "now", "that", "this", "these", "those", "let"
    ));

    private final Set<String> businessKeywords = new HashSet<>(Arrays.asList(
        "project", "timeline", "milestone", "deadline", "budget", "cost", "expense", "revenue",
        "client", "customer", "vendor", "partner", "stakeholder", "team", "resource",
        "deliverable", "requirement", "specification", "plan", "strategy", "tactic",
        "objective", "goal", "metric", "kpi", "report", "analysis", "data", "implementation",
        "development", "design", "testing", "deployment", "production", "schedule", "progress",
        "sprint", "agile", "waterfall", "product", "service", "market", "competitor",
        "출시", "완료", "피드백", "조정", "공유", "논의", "확정", "일주일", "사용자", "테스트 계획", "개발 완료", "디자인 확정", "문서 작성"
    ));

    private final Pattern importantTermPattern = Pattern.compile("(?:[A-Z][a-z]+\\s*)+|(?:[A-Z]{2,})|(?:[A-Za-z]+(?:\\.[A-Za-z]+)+)");

    public Set<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptySet();
        }

        log.debug("Extracting keywords from text: {}", text);
        Set<String> keywords = new HashSet<>();

        // 비즈니스 키워드 추출
        for (String keyword : businessKeywords) {
            if (text.toLowerCase().contains(keyword.toLowerCase())) {
                keywords.add(keyword);
                log.debug("Found business keyword: {}", keyword);
            }
        }

        // 중요한 단어 추출
        Matcher matcher = importantTermPattern.matcher(text);
        while (matcher.find()) {
            String term = matcher.group().trim();
            if (term.length() > 2 && !stopWords.contains(term.toLowerCase())) {
                keywords.add(term);
                log.debug("Found important term: {}", term);
            }
        }

        // 텍스트에서 토큰 분리 및 불용어 필터링
        String[] tokens = text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\uAC00-\uD7AF\s]", " ")
                .split("\s+");

        Set<String> generalKeywords = Arrays.stream(tokens)
                .filter(word -> word.length() > 2)
                .filter(word -> !stopWords.contains(word))
                .collect(Collectors.toSet());

        keywords.addAll(generalKeywords);

        // 키워드의 최대 개수는 7개로 제한
        if (keywords.size() > 7) {
            keywords = keywords.stream()
                    .limit(7)
                    .collect(Collectors.toSet());
        }

        log.debug("Extracted keywords: {}", keywords);
        return keywords;
    }
}

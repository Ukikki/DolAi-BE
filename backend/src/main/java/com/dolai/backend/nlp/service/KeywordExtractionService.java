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

    // Common words to filter out
    private final Set<String> stopWords = new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", "be", "have", "has",
            "had", "do", "does", "did", "to", "from", "in", "out", "on", "off", "over", "under",
            "again", "further", "then", "once", "here", "there", "when", "where", "why", "how",
            "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no",
            "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can",
            "will", "just", "don", "should", "now", "that", "this", "these", "those", "let"
    ));

    // Business-specific keywords
    private final Set<String> businessKeywords = new HashSet<>(Arrays.asList(
            "project", "timeline", "milestone", "deadline", "budget", "cost", "expense", "revenue",
            "client", "customer", "vendor", "partner", "stakeholder", "team", "resource",
            "deliverable", "requirement", "specification", "plan", "strategy", "tactic",
            "objective", "goal", "metric", "kpi", "report", "analysis", "data", "implementation",
            "development", "design", "testing", "deployment", "production", "schedule", "progress",
            "sprint", "agile", "waterfall", "product", "service", "market", "competitor",
            "프로젝트", "일정", "마일스톤", "마감일", "예산", "비용", "지출", "수익",
            "고객", "공급업체", "파트너", "이해관계자", "팀", "자원", "산출물",
            "요구사항", "명세", "계획", "전략", "전술", "목표", "지표", "보고서",
            "분석", "데이터", "구현", "개발", "설계", "테스트", "배포", "생산",
            "일정", "진행", "스프린트", "애자일", "워터폴", "제품", "서비스", "시장", "경쟁사"
    ));

    // Pattern for finding important terms like technical terms or proper nouns
    private final Pattern importantTermPattern = Pattern.compile("(?:[A-Z][a-z]+\\s*)+|(?:[A-Z]{2,})|(?:[A-Za-z]+(?:\\.[A-Za-z]+)+)");

    /**
     * Extracts keywords from text
     */
    public Set<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptySet();
        }

        log.debug("Extracting keywords from text: {}", text);
        Set<String> keywords = new HashSet<>();

        // Extract business keywords
        for (String keyword : businessKeywords) {
            if (text.toLowerCase().contains(keyword.toLowerCase())) {
                keywords.add(keyword);
                log.debug("Found business keyword: {}", keyword);
            }
        }

        // Extract important terms (technical terms, proper nouns)
        Matcher matcher = importantTermPattern.matcher(text);
        while (matcher.find()) {
            String term = matcher.group().trim();
            if (term.length() > 2 && !stopWords.contains(term.toLowerCase())) {
                keywords.add(term);
                log.debug("Found important term: {}", term);
            }
        }

        // Tokenize and clean text for general keywords
        String[] tokens = text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9가-힣\\s]", " ")
                .split("\\s+");

        // Extract general keywords (remove stopwords and short words)
        Set<String> generalKeywords = Arrays.stream(tokens)
                .filter(word -> word.length() > 2)
                .filter(word -> !stopWords.contains(word))
                .collect(Collectors.toSet());

        keywords.addAll(generalKeywords);

        // Limit number of keywords to prevent excessive nodes
        if (keywords.size() > 5) {
            keywords = keywords.stream()
                    .limit(5)
                    .collect(Collectors.toSet());
        }

        log.debug("Extracted keywords: {}", keywords);
        return keywords;
    }
}
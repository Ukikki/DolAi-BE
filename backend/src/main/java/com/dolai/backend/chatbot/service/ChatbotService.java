package com.dolai.backend.chatbot.service;

import com.dolai.backend.chatbot.model.ChatRequest;
import com.dolai.backend.chatbot.model.ChatResponse;
import com.dolai.backend.common.success.ChatPromptBuilder;
import com.dolai.backend.stt_log.model.STTLog;
import com.dolai.backend.stt_log.repository.STTLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatbotService {
    @Qualifier("geminiRestTemplate")
    @Autowired
    private final RestTemplate restTemplate;
    private final STTLogRepository sttLogRepository;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public String getContents(String prompt) {

        // Gemini에 요청 전송
        String requestUrl = apiUrl + "?key=" + geminiApiKey;

        ChatRequest request = new ChatRequest(prompt);
        ChatResponse response = restTemplate.postForObject(requestUrl, request, ChatResponse.class);

        String message = response.getCandidates().get(0).getContent().getParts().get(0).getText().toString();

        return message;
    }

    public String getSmartResponse(String meetingId, String userQuestion) {
        List<STTLog> logs = sttLogRepository.findByMeetingIdOrderByTimestampAsc(meetingId);

        if (logs.size() > 30) {
            logs = logs.subList(logs.size() - 30, logs.size()); // 최근 30개만 사용
        }

        String fullPrompt = ChatPromptBuilder.buildPromptWithContext(logs, userQuestion);

        return getContents(fullPrompt);
    }
}
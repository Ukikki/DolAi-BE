package com.dolai.backend.common.success;


import com.dolai.backend.stt_log.model.STTLog;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ChatPromptBuilder {

    public static String buildPromptWithContext(List<STTLog> logs, String userQuestion) {
        String context = logs.stream()
                .map(log -> String.format("%s %s: %s",
                        log.getTimestamp().toLocalTime().withNano(0),
                        log.getSpeakerName(),
                        log.getText()))
                .collect(Collectors.joining("\n"));
        String systemPrompt = "당신은 회의의 AI 비서입니다. 질문자와 대화하는 형식으로 대답하세요.\n" +
                "회의 참가자들의 대화 내용을 듣고, 최대한 핵심만 담아 간결하게 답해주세요.\n" +
                "항상 실시간 회의 데이터를 기준으로 답하고, 불필요한 답변은 피하세요.\n" +
                "ex Q: 지금 한지운씨가 뭐라고 말했어? 난 뭐라고 대답해야하지?\n" +
                "A: 한지운씨는 방금 ~ 라고 말씀하셨습니다. " +
                "\"~\" 라고 대답하시면 좋을 것 같아요. ";
        String fullPrompt = systemPrompt + "\n\n실시간 회의 내용:\n" + context + "\n\n질문: " + userQuestion;
        log.info("Generated Full Prompt: \n{}", fullPrompt);
        return fullPrompt;
    }
}

package com.dolai.backend.llm;

import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.stt_log.model.STTLog;
import com.dolai.backend.stt_log.repository.STTLogRepository;
import com.dolai.backend.todo.model.AiTodoDto;
import com.dolai.backend.todo.model.Todo;
import com.dolai.backend.todo.model.TodoRequestDto;
import com.dolai.backend.todo.repository.TodoRepository;
import com.dolai.backend.user.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.dolai.backend.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmTodoService {

    private final LlmService llmService;
    private final UserRepository userRepository;
    private final TodoRepository todoRepository;
    private final STTLogRepository sttLogRepository;
    private final ObjectMapper objectMapper;

    public void extractAndSaveTodos(String meetingId) {
        List<STTLog> recentLogs = sttLogRepository.findUncheckedLogsByMeetingId(meetingId);
        log.info("Found {} logs for 미팅아이디: {}", recentLogs.size(), meetingId);
        if (recentLogs.isEmpty()) {
            log.warn("No unchecked logs found for meetingId: {}", meetingId);
            return;
        }

        String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        StringBuilder promptBuilder = new StringBuilder("""
            현재 시간은 %s입니다.

            아래는 회의에서 최근에 있었던 대화입니다.
            할 일을 요청한 내용이 있다면 JSON 배열로 추출해주세요.
            마감일은 반드시 ISO 8601 형식인 yyyy-MM-dd'T'HH:mm:ss 형태로 작성해 주세요. \s
            형식:
            [
              {
                "speaker": "이름",
                "task": "할 일 내용",
                "dueDate" : "2025-05-20T23:59:59"
              }
            ]

            할 일이 없다면 빈 배열을 반환하세요.

            대화:
        """.formatted(now));

        for (STTLog log : recentLogs) {
            String time = log.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String text = (log.getTextKo() != null && !log.getTextKo().isBlank()) ? log.getTextKo() : log.getText();
            promptBuilder.append("- ").append(log.getSpeakerName()).append(": ").append(text)
                    .append(" (").append(time).append(")\n");
        }

        String prompt = promptBuilder.toString();

        try {
            String rawResponse = llmService.ask(prompt, List.of()).block();
            log.info("🔁 LLM raw response:\n{}", rawResponse);

            String cleaned = llmService.cleanJsonResponse(rawResponse);
            if (cleaned == null || !cleaned.trim().startsWith("[")) {
                log.warn("⚠️ 정제된 응답이 JSON 배열 아님. 응답: {}", cleaned);
                return;
            }

            List<AiTodoDto> todoList = objectMapper.readValue(cleaned, new TypeReference<>() {});
            log.info("✅ 파싱된 Todo 수: {}", todoList.size());

            for (AiTodoDto dto : todoList) {
                String speaker = dto.getSpeaker();
                String task = dto.getTask();

                // dueDate 처리
                String dueDateStr = dto.getDueDate();
                LocalDateTime dueDate = null;
                if (dueDateStr != null && !dueDateStr.isBlank()) {
                    try {
                        dueDate = LocalDateTime.parse(dueDateStr);
                    } catch (DateTimeParseException e) {
                        log.warn("⚠️ 날짜 파싱 실패: '{}'", dueDateStr);
                    }
                } else {
                    log.warn("⚠️ 유효하지 않은 dueDate: '{}'", dueDateStr);
                }

                Optional<User> userOpt = userRepository.findByName(speaker);
                if (userOpt.isEmpty()) {
                    log.warn("⚠️ '{}' 사용자 이름에 해당하는 유저를 찾을 수 없습니다. Todo 건너뜀", speaker);
                    continue;
                }

                User user = userOpt.get();
                Meeting meeting = recentLogs.get(0).getMeeting(); // 모든 로그는 동일 미팅

                Todo todo = Todo.create(user, TodoRequestDto.builder()
                        .title(task)
                        .dueDate(dueDate)
                        .meetingId(meeting.getId().toString())
                        .assignee(user.getName())
                        .build(), meeting);

                todoRepository.save(todo);
                log.info("📝 저장된 Todo: {}", todo);
            }

            recentLogs.forEach(log -> log.setTodoChecked(true));
            sttLogRepository.saveAll(recentLogs);
            log.info("🆗 {}개의 로그를 todoChecked = true로 업데이트", recentLogs.size());

        } catch (Exception e) {
            log.error("❌ Gemini Todo 추출 실패: {}", e.getMessage(), e);
        }
    }
}

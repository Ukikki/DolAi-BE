package com.dolai.backend.llm;

import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.notification.model.enums.Type;
import com.dolai.backend.notification.service.NotificationService;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmTodoService {

    private final LlmService llmService;
    private final TodoRepository todoRepository;
    private final STTLogRepository sttLogRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public void extractAndSaveTodos(String meetingId) {

        List<STTLog> recentLogs = sttLogRepository.findUncheckedLogsByMeetingId(meetingId);
        log.info("Found {} logs for 미팅아이디: {}", recentLogs.size(), meetingId);
        if (recentLogs.isEmpty()) {
            log.warn("No unchecked logs found for meetingId: {}", meetingId);
            return;
        }

        Meeting meeting = recentLogs.get(0).getMeeting(); // recentLogs가 비어있지 않으니 안전

        // 참가자 목록 문자열로 만들기
        String userList = meeting.getParticipants().stream()
                .map(p -> "\"" + p.getUser().getName() + "\"")
                .collect(Collectors.joining(", "));
        String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String promptTemplate = """
                    현재 시간은 %s입니다.
                
                    아래는 회의에서 최근에 있었던 대화입니다.
                    이 회의에는 다음 참가자들이 있습니다: %s
                    할 일을 요청한 내용이 있다면 JSON 배열로 추출해주세요.
                    Whisper가 STT한 로그이므로 발화와 가장 유사한 참가자 명을 'name'에 그대로 작성하세요.
                    만약 일치하는 name이 없다면 발화자의 이름을 'name'에 그대로 넣으세요.
                    마감일은 반드시 ISO 8601 형식인 yyyy-MM-dd'T'HH:mm:ss 형태로 작성해 주세요. 
                    형식:
                    [
                      {
                        "name": "이름",
                        "task": "할 일 내용",
                        "dueDate" : "2025-05-20T23:59:59"
                      }
                    ]
                
                    할 일이 없다면 빈 배열을 반환하세요.
                
                    대화:
                """;

        String promptHeader = String.format(promptTemplate, now, userList);

// 이제 StringBuilder 사용
        StringBuilder promptBuilder = new StringBuilder(promptHeader);


        for (STTLog log : recentLogs) {
            String time = log.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String text = (log.getTextKo() != null && !log.getTextKo().isBlank()) ? log.getTextKo() : log.getText();
            promptBuilder.append("- ").append(log.getSpeakerName()).append(": ").append(text)
                    .append(" (").append(time).append(")\n");
        }

        String prompt = promptBuilder.toString();
        log.info("📌 미팅 ID: {}", meetingId);
        log.info("👥 회의 참가자 목록: {}", userList);
        log.debug("🧾 최종 프롬프트:\n{}", prompt);

        try {
            log.info("🔁 LLM 호출 시작...");
            String rawResponse = llmService.ask(prompt, List.of()).block();
            log.info("✅ LLM 응답 수신 완료");
            log.debug("🔁 LLM raw response:\n{}", rawResponse);

            String cleaned = llmService.cleanJsonResponse(rawResponse);
            if (cleaned == null || !cleaned.trim().startsWith("[")) {
                log.warn("⚠️ 정제된 응답이 JSON 배열 아님. 응답: {}", cleaned);
                return;
            }

            List<AiTodoDto> todoList = objectMapper.readValue(cleaned, new TypeReference<>() {});
            log.info("✅ 파싱된 Todo 수: {}", todoList.size());

            for (AiTodoDto dto : todoList) {
                String task = dto.getTask();
                String dueDateStr = dto.getDueDate();
                String llmAssigneeName = dto.getName();

                log.info("🧪 추출된 할 일 항목: {}", dto);
                log.info("🧾 LLM 추출 이름(name): {}", llmAssigneeName);

                LocalDateTime dueDate = null;
                if (dueDateStr != null && !dueDateStr.isBlank()) {
                    try {
                        dueDate = LocalDateTime.parse(dueDateStr);
                    } catch (DateTimeParseException e) {
                        log.warn("⚠️ 날짜 파싱 실패: '{}'", dueDateStr);
                    }
                }

                // 1. 할 일 저장: dto.name과 일치하는 참가자에게만
                Optional<User> matchedUserOpt = meeting.getParticipants().stream()
                        .map(p -> p.getUser())
                        .filter(u -> u.getName().equalsIgnoreCase(llmAssigneeName)) // 이름 비교 (대소문자 무시)
                        .findFirst();

                if (matchedUserOpt.isPresent()) {
                    User matchedUser = matchedUserOpt.get();
                    log.info("✅ '{}' 에게 할 일 저장됨", matchedUser.getName());

                    Todo todo = Todo.create(matchedUser, TodoRequestDto.builder()
                            .title(task)
                            .dueDate(dueDate)
                            .meetingId(meeting.getId())
                            .assignee(matchedUser.getName())
                            .build(), meeting);

                    todoRepository.save(todo);
                    log.info("📝 저장 완료: {}", todo);
                } else {
                    log.warn("⚠️ '{}' 이름과 일치하는 유저를 찾지 못해 저장 생략", llmAssigneeName);
                }

                // 2. 모든 참가자에게 알림 전송
                for (var participant : meeting.getParticipants()) {
                    User user = participant.getUser();
                    log.info("📢 알림 발송: {}님에게 '{}' 할 일 알림 전송", user.getName(), task);
                    notificationService.notifyDolAi(
                            meetingId,
                            Type.TODO_CREATED,
                            Map.of(
                                    "assignee", user.getName(),  // DTO에서 추출한 이름 그대로 전달
                                    "todo", task
                            ),
                            null
                    );
                }


            }

            recentLogs.forEach(log -> log.setTodoChecked(true));
            sttLogRepository.saveAll(recentLogs);
            log.info("🆗 {}개의 STT 로그 todoChecked = true로 변경됨", recentLogs.size());

        } catch (Exception e) {
            log.error("❌ Gemini Todo 추출 실패: {}", e.getMessage(), e);
        }
    }
}
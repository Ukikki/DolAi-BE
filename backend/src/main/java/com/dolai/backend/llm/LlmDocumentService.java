package com.dolai.backend.llm;

import com.dolai.backend.document.service.MeetingDocGenerator;
import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.meeting.repository.MeetingRepository;
import com.dolai.backend.s3.S3UploadService;
import com.dolai.backend.stt_log.service.STTLogService;
import com.dolai.backend.todo.model.Todo;
import com.dolai.backend.todo.repository.TodoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmDocumentService {

    private final STTLogService sttLogService;
    private final LlmService llmService;
    private final MeetingDocGenerator meetingDocGenerator;
    private final MeetingRepository meetingRepository;
    private final ObjectMapper objectMapper;
    private final AzureTranslationService azureTranslationService;
    private final TodoRepository todoRepository;
    private final S3UploadService s3UploadService;

    @Value("${doc.template-dir}")
    private String templateDir;

    @Value("${doc.output-dir}")
    private String outputDir;

    public Map<String, Map<String, String>> summarizeAndGenerateDoc(String meetingId) {
        // 1. 회의 로그 가져오기
        String context = sttLogService.getFullTranscriptForLLM(meetingId);

        // 2. 프롬프트 구성
        String prompt = """
                회의 전체 대화를 분석하여 회의록을 작성합니다.
                다음 JSON 형식으로 정리해주세요.
                
                - 각 항목은 주제(title)와 본문(body)으로 구성됩니다.
                - body는 **한두 문장씩 간결하게 핵심만 정리한 bullet 형식**으로 작성해주세요.
                - 줄바꿈은 <w:br/> 으로 표현합니다.
                - 결정사항(result)은 핵심만 간결하게 bullet 형식으로 나열해주세요.
                
                정확한 JSON 예시 형식:
                {
                  "content": [
                    {
                      "title": "Whisper 성능 저하",
                      "body": "- Docker 컨테이너에서 GPU 미인식으로 인한 속도 저하 발생.\\n- EC2 T4 인스턴스에서 Whisper tiny 모델 CPU fallback 확인."
                    },
                    {
                      "title": "이미지 설정 문제",
                      "body": "- base image는 nvidia/cuda:12.2.0-runtime 사용 중.\\n- nvidia-smi 명령어 작동 안 됨."
                    }
                  ],
                  "result": [
                    "base image를 devel로 변경하여 테스트",
                    "로깅 레벨 조절",
                    "GPU 인식 테스트 진행"
                  ]
                }
                
                이 형식을 정확히 지켜주세요. **JSON 외 텍스트는 절대 출력하지 마세요.**
                
                회의 로그:
                """ + context;

        Map<String, String> summary = Map.of(
                "content", "(요약 없음)",
                "result", "(결정사항 없음)"
        );

        try {
            String llmResponse = llmService.ask(prompt, List.of(prompt)).block();

            if (llmResponse == null || llmResponse.isBlank()) {
                log.warn("⚠️ Gemini 응답이 비어 있습니다.");
            } else {
                log.debug("🔥 Gemini 응답 원본:\n{}", llmResponse);

                String cleaned = llmResponse.replaceAll("(?i)```json|```", "").trim();

                LlmSummaryDto dto = objectMapper.readValue(cleaned, LlmSummaryDto.class);
                StringBuilder contentBuilder = new StringBuilder();
                for (LlmSummaryDto.Section section : dto.getContent()) {
                    contentBuilder.append("■ ").append(section.getTitle()).append("\n");

                    String[] lines = section.getBody().split("\\\\n"); // <-- 중요: \n은 \\n으로 들어옴
                    for (String line : lines) {
                        contentBuilder.append(line.trim()).append("\n");
                    }

                    contentBuilder.append("\n"); // 주제 간 공백
                }
                String contentText = contentBuilder.toString().trim().replace("\n", "<w:br/>");

                String resultText = "- " + String.join("\n- ", dto.getResult()).replace("\n", "<w:br/>");

                summary = Map.of(
                        "content", contentText,
                        "result", resultText
                );
            }
        } catch (Exception e) {
            log.error("❌ Gemini 응답 처리 실패: {}", e.getMessage(), e);
        }

        // 3. 회의 정보 가져오기
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("회의를 찾을 수 없습니다: " + meetingId));

        //참석자 이름 리스트
        String attendees = meeting.getParticipants().stream()
                .map(p -> p.getUser().getName())
                .collect(Collectors.joining(", "));

        String date = meeting.getStartTime().format(DateTimeFormatter.ofPattern("yyyy.M.d"));

        String time = meeting.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " ~ " +
                meeting.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));

        List<Todo> todoList = todoRepository.findByMeetingId(meetingId);

        String todoListString = todoList.stream()
                .map(todo -> "• " + todo.getUser().getName() + ": " + todo.getTitle() + " - " +
                        todo.getDueDate().format(DateTimeFormatter.ofPattern("MM.dd HH:mm")))
                .collect(Collectors.joining("<w:br/>"));

        //System.out.println("Todo List String: " + todoListString);

        // 영어 투두
        String todoListStringEn = todoList.stream()
                .map(todo -> {
                    String translatedTitle = azureTranslationService.translate(todo.getTitle(), "en");
                    return "• " + todo.getUser().getName() + ": " + translatedTitle + " - " +
                            todo.getDueDate().format(DateTimeFormatter.ofPattern("MM.dd HH:mm"));
                })
                .collect(Collectors.joining("<w:br/>"));

        // 중국어 투두
        String todoListStringZh = todoList.stream()
                .map(todo -> {
                    String translatedTitle = azureTranslationService.translate(todo.getTitle(), "zh-Hans");
                    return "• " + todo.getUser().getName() + ": " + translatedTitle + " - " +
                            todo.getDueDate().format(DateTimeFormatter.ofPattern("MM.dd HH:mm"));
                })
                .collect(Collectors.joining("<w:br/>"));

        // 4. 문서에 쓸 값 구성
        String contentText = summary.getOrDefault("content", "(요약 없음)").replace("\n", "<w:br/>");
        String resultText = summary.getOrDefault("result", "(결정사항 없음)").replace("\n", "<w:br/>");

        Map<String, String> values = Map.of(
                "date", date,
                "time", time,
                "title", meeting.getTitle(),
                "attendees", attendees,
                "content", contentText,
                "result", resultText,
                "todolist", todoListString
        );

        // 5. 문서 생성
        File template = new File(templateDir, "meetingDoc.docx");
        String safeTitle = meeting.getTitle().replaceAll("[\\\\/:*?\"<>|\\s]", "_");
        String fileName = safeTitle + ".docx";

        Map<String, Map<String, String>> docInfo = new HashMap<>();  // 변경: 중첩 Map 사용

        File output = new File(outputDir, fileName);
        output.getParentFile().mkdirs();

        meetingDocGenerator.generateDocx(values, template, output);
        String s3Url = s3UploadService.uploadMeetingDocument(output, meetingId, fileName);
        Map<String, String> koInfo = new HashMap<>();
        koInfo.put("url", s3Url);
        koInfo.put("title", meeting.getTitle());
        docInfo.put("ko", koInfo);

        // 6. 번역된 문서 생성
        String s3UrlEn = null;
        String s3UrlZh = null;

        try {
            String titleEn = azureTranslationService.translate(meeting.getTitle(), "en");  // 영어로 번역
            String titleZh = azureTranslationService.translate(meeting.getTitle(), "zh-Hans");  // 중국어로 번역

            String safeTitleEn = titleEn.replaceAll("[\\\\/:*?\"<>|\\s]", "_");
            String safeTitleZh = titleZh.replaceAll("[\\\\/:*?\"<>|\\s]", "_");

            String fileNameEn = safeTitleEn + ".docx";
            String fileNameZh = safeTitleZh + ".docx";

            String contentEn = azureTranslationService.translate(contentText, "en");
            String resultEn = azureTranslationService.translate(resultText, "en");

            String contentZh = azureTranslationService.translate(contentText, "zh-Hans");
            String resultZh = azureTranslationService.translate(resultText, "zh-Hans");

            Map<String, String> valuesEn = Map.of(
                    "date", date,
                    "time", time,
                    "title", titleEn,
                    "attendees", attendees,
                    "content", contentEn,
                    "result", resultEn,
                    "todolist", todoListStringEn
            );

            Map<String, String> valuesZh = Map.of(
                    "date", date,
                    "time", time,
                    "title", titleZh,
                    "attendees", attendees,
                    "content", contentZh,
                    "result", resultZh,
                    "todolist", todoListStringZh
            );

            // 영어 문서 생성 및 S3 업로드
            File outputEn = generateDocx(valuesEn, fileNameEn, "meetingDoc_en.docx");
            s3UrlEn = s3UploadService.uploadMeetingDocument(outputEn, meetingId, fileNameEn);
            Map<String, String> enInfo = new HashMap<>();
            enInfo.put("url", s3UrlEn);
            enInfo.put("title", titleEn);
            docInfo.put("en", enInfo);

            // 중국어 문서 생성 및 S3 업로드
            File outputZh = generateDocx(valuesZh, fileNameZh, "meetingDoc_zh.docx");
            s3UrlZh = s3UploadService.uploadMeetingDocument(outputZh, meetingId, fileNameZh);
            Map<String, String> zhInfo = new HashMap<>();
            zhInfo.put("url", s3UrlZh);
            zhInfo.put("title", titleZh);
            docInfo.put("zh", zhInfo);
        } catch (Exception e) {
            log.error("❌ 다국어 번역 또는 문서 생성 실패", e);
        }
        return docInfo;
    }

    private File generateDocx(Map<String, String> values, String filename, String templateFilename) {
        File template = new File(templateDir, templateFilename);
        File output = new File(outputDir, filename);
        output.getParentFile().mkdirs();
        meetingDocGenerator.generateDocx(values, template, output);
        return output;  // 생성된 파일 객체 반환
    }
}

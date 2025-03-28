package com.dolai.backend.document.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.document.model.Document;
import com.dolai.backend.document.model.DocumentResponseDto;
import com.dolai.backend.document.repository.DocumentRepository;
import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.meeting.repository.MeetingRepository;
import com.dolai.backend.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final MeetingRepository meetingRepository;
    private final TodoService todoService;
    private final DocumentRepository documentRepository;
    private final S3ServiceStub s3Service;

    @Transactional(readOnly = true)
    public DocumentResponseDto getDocumentByMeetingId(String meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));

        Document document = documentRepository.findTopByMeetingOrderByVersionDesc(meeting)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));

        List<String> participants = meeting.getParticipants().stream()
                .map(p -> p.getUser().getEmail())
                .toList();

        // 예시 URL들 (실제론 Document에 저장된 경로일 수 있음)
        String detailedContentUrl = document.getFileUrl().replace(".pdf", ".json");
        String graphImageUrl = document.getFileUrl().replace(".pdf", "_graph.png");
        String notesUrl = document.getFileUrl().replace(".pdf", "_notes.txt");

        return DocumentResponseDto.builder()
                .version(document.getVersion())
                .title(document.getTitle())
                .date(meeting.getStartTime().toLocalDate().toString())
                .participants(participants)
                .duration(String.valueOf(Duration.between(meeting.getStartTime(), meeting.getEndTime()).toMinutes()))
                .organizer("김철수") // 예시, 실제 hostUserId → User 이름 매핑 가능
                .summary(document.getSummary())
                .detailedContent(s3Service.fetchDetailedContent(detailedContentUrl))
                .graphs(s3Service.fetchGraphImageBase64(graphImageUrl))
                .notes(s3Service.fetchNotes(notesUrl))
                .todoList(todoService.getTodosByMeeting(meetingId))
                .documentUrl(document.getFileUrl()) // PDF 다운로드 링크
                .build();
    }
}

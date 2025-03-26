package com.dolai.backend.document.service;

import com.dolai.backend.document.repository.DocumentRepository;
import com.dolai.backend.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final MeetingRepository meetingRepository;
/*
    @Override
    public List<DocumentResponseDto> getDocumentsByMeetingId(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));

        return documentRepository.findByMeeting(meeting)
                .stream()
                .map(doc -> new DocumentResponseDto(
                        doc.getId(),
                        doc.getMeeting().getId(),
                        doc.getTitle(),
                        doc.getSummary(),
                        doc.getFileUrl()
                ))
                .collect(Collectors.toList());
    }


    // 문서 생성
    @Override
    public DocumentResponseDto createDocument(DocumentRequest requestDto) {
        Meeting meeting = meetingRepository.findById(requestDto.getMeetingId())  //  Meeting 객체 조회
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));

        Document document = Document.builder()
                .meeting(meeting)  // ✅ Meeting 객체를 넣음
                .title(requestDto.getTitle())
                .summary(requestDto.getSummary())
                .fileUrl("S3_URL_PLACEHOLDER")  // S3에 저장 후 URL을 업데이트할 예정
                .build();

        documentRepository.save(document);

        return new DocumentResponseDto(
                document.getId(),
                document.getMeeting().getId(),
                document.getTitle(),
                document.getSummary(),
                document.getFileUrl()
        );

     }
     */
}

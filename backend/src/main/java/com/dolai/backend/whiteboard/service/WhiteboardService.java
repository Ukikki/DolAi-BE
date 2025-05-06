package com.dolai.backend.whiteboard.service;

import com.dolai.backend.whiteboard.model.WhiteboardSession;
import com.dolai.backend.whiteboard.repository.WhiteboardSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WhiteboardService {
    private final WhiteboardSessionRepository repository;

    // 화이트보드 시작: 세션 생성
    public WhiteboardSession startSession(String meetingId) {
        WhiteboardSession session = WhiteboardSession.builder()
                .meetingId(meetingId)
                .startTime(LocalDateTime.now())
                .active(true)
                .build();
        return repository.save(session);
    }
}

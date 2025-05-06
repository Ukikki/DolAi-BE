package com.dolai.backend.whiteboard.service;

import com.dolai.backend.whiteboard.model.WhiteboardSession;
import com.dolai.backend.whiteboard.repository.WhiteboardSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

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

    // 화이트보드 종료: 세션 종료
    public WhiteboardSession endSession(String meetingId) {
        Optional<WhiteboardSession> optional = repository.findById(meetingId);
        if (optional.isPresent()) {
            WhiteboardSession session = optional.get();
            session.setEndTime(LocalDateTime.now());
            session.setActive(false);
            return repository.save(session);
        }
        throw new IllegalArgumentException("세션을 찾을 수 없습니다: " + meetingId);
    }
}

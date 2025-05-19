package com.dolai.backend.screenshare.service;

import com.dolai.backend.screenshare.model.ScreenShare;
import com.dolai.backend.screenshare.repository.ScreenShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScreenShareService {
    private final ScreenShareRepository repository;

    // 화면 공유 시작: 세션 생성
    public ScreenShare start(String meetingId, String userId) {
        ScreenShare session = ScreenShare.builder()
                .meetingId(meetingId)
                .userId(userId)
                .startTime(LocalDateTime.now())
                .active(true)
                .build();
        return repository.save(session);
    }

    // 화면 공유 종료: 세션 종료
    public ScreenShare stop(String meetingId, String userId, String text, String timestamp) {
        return repository.findById(meetingId)
                .map(session -> {
                    session.setEndTime(LocalDateTime.now());
                    session.setActive(false);
                    session.setOcrText(text);
                    session.setOcrTimestamp(timestamp);
                    return repository.save(session);
                })
                .orElseThrow(() -> new IllegalArgumentException("화면 공유 세션을 찾을 수 없습니다: " + meetingId));
    }

    // 화면 공유 상태 조회
    public Optional<ScreenShare> get(String meetingId) {
        return repository.findById(meetingId);
    }
}


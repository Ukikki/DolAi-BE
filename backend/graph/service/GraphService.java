package graph.service;

import graph.edge.ParticipatedIn;
import graph.edge.SpokeIn;
import graph.edge.SpokenBy;
import graph.entity.MeetingNode;
import graph.entity.SpeakerNode;
import graph.entity.UtteranceNode;
import graph.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphService {

    private final MeetingNodeRepository meetingNodeRepository;
    private final SpeakerNodeRepository speakerNodeRepository;
    private final UtteranceNodeRepository utteranceNodeRepository;

    private final ParticipatedInRepository participatedInRepository;
    private final SpokenByRepository spokenByRepository;
    private final SpokeInRepository spokeInRepository;

    public void processSttLog(String meetingId, String speaker, String text, LocalDateTime timestamp) {
        // 1. 회의 노드 저장 또는 조회
        MeetingNode meeting = meetingNodeRepository.findById(meetingId)
                .orElseGet(() -> {
                    MeetingNode newMeeting = MeetingNode.builder()
                            .id(meetingId)
                            .title("Untitled Meeting") // 제목 없으면 기본값
                            .startTime(timestamp)
                            .build();
                    return meetingNodeRepository.save(newMeeting);
                });

        // 2. 화자 노드 저장 또는 조회
        SpeakerNode speakerNode = speakerNodeRepository.findById(speaker)
                .orElseGet(() -> {
                    SpeakerNode newSpeaker = SpeakerNode.builder()
                            .id(speaker)
                            .name(speaker)
                            .build();
                    return speakerNodeRepository.save(newSpeaker);
                });

        // 3. 발화 노드 저장
        UtteranceNode utterance = UtteranceNode.builder()
                .text(text)
                .timestamp(timestamp)
                .build();
        utterance = utteranceNodeRepository.save(utterance);

        // 4. 엣지 저장
        participatedInRepository.save(new ParticipatedIn(null, speakerNode, meeting));
        spokenByRepository.save(new SpokenBy(null, utterance, speakerNode));
        spokeInRepository.save(new SpokeIn(null, utterance, meeting));

        log.
        log.info("미팅 로그 - ID: " + meetingId + ", 화자: " + speaker + ", 발화: " + text + ", 시간: " + timestamp);
    }
}

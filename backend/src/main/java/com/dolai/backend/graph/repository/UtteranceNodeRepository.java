package com.dolai.backend.graph.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.dolai.backend.graph.entity.UtteranceNode;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UtteranceNodeRepository extends ArangoRepository<UtteranceNode, String> {
    boolean existsByMeetingIdAndSpeakerNameAndText(
            @Param("meetingId") String meetingId,
            @Param("speakerName") String speakerName,
            @Param("text") String text);

    Optional<UtteranceNode> findByMeetingIdAndSpeakerNameAndText(
            @Param("meetingId") String meetingId,
            @Param("speakerName") String speakerName,
            @Param("text") String text);
}
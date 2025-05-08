package com.dolai.backend.graph.repository;

import com.arangodb.ArangoDatabase;
import com.dolai.backend.graph.model.UtteranceNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Repository
@RequiredArgsConstructor
public class UtteranceRepository {

    private final ArangoDatabase arangoDatabase;

    public List<UtteranceNode> findByMeetingId(String meetingId) {
        String query = """
            FOR u IN utterances
                FILTER u.meetingId == @meetingId
                RETURN u
        """;

        Map<String, Object> bindVars = Map.of("meetingId", meetingId);

        return StreamSupport.stream(
                arangoDatabase.query(query, bindVars, null, UtteranceNode.class).spliterator(), false
        ).collect(Collectors.toList());
    }
}

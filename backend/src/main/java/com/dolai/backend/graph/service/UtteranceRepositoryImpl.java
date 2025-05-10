package com.dolai.backend.graph.repository;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.BaseDocument;
import com.dolai.backend.graph.model.UtteranceNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class UtteranceRepositoryImpl {

    private final ArangoDatabase arangoDatabase;

    public List<UtteranceNode> findByMeetingId(String meetingId) {
        String query = """
            FOR u IN utterances
                FILTER u.meetingId == @meetingId
                SORT u.startTime ASC
                RETURN u
        """;

        Map<String, Object> bindVars = Map.of("meetingId", meetingId);

        var cursor = arangoDatabase.query(query, bindVars, null, Map.class);

        List<UtteranceNode> results = new ArrayList<>();
        for (Object raw : cursor) {
            @SuppressWarnings("unchecked")
            Map<String, Object> doc = (Map<String, Object>) raw;

            UtteranceNode utterance = new UtteranceNode();
            utterance.setId((String) doc.get("_key"));
            utterance.setMeetingId((String) doc.get("meetingId"));
            utterance.setSpeaker((String) doc.get("speaker"));
            utterance.setText((String) doc.get("text"));

            Object startTime = doc.get("startTime");
            if (startTime instanceof Number) {
                utterance.setStartTime(((Number) startTime).longValue());
            }
            Object endTime = doc.get("endTime");
            if (endTime instanceof Number) {
                utterance.setEndTime(((Number) endTime).longValue());
            }

            results.add(utterance);
        }

        return results;
    }
}

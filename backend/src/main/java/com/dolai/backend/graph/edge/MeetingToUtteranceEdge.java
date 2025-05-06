package com.dolai.backend.graph.edge;

import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;
import lombok.*;
import org.springframework.data.annotation.Id;


@Edge("meeting_to_utterance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MeetingToUtteranceEdge {
    @Id
    private String id;

    @From
    private String _from;

    @To
    private String _to;

    public MeetingToUtteranceEdge(String from, String to) {
        this._from = from;
        this._to = to;
    }
}
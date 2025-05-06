package com.dolai.backend.graph.edge;

import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;
import lombok.*;
import org.springframework.data.annotation.Id;

@Edge("utterance_to_speaker")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UtteranceToSpeakerEdge {
    @Id
    private String id;

    @From
    private String _from;

    @To
    private String _to;

    public UtteranceToSpeakerEdge(String from, String to) {
        this._from = from;
        this._to = to;
    }
}

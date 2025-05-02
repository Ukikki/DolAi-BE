package com.example.demo.graph.edge;

import com.arangodb.springframework.annotation.Edge;
import org.springframework.data.annotation.Id;
import lombok.*;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;

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

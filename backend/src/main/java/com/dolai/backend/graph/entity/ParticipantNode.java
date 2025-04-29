package com.dolai.backend.graph.entity;

import com.arangodb.springframework.annotation.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

/**
 * 회의 참석자를 나타내는 Node
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document("participant_nodes")
public class ParticipantNode {
    @Id
    private String id;
    private String name;
    private String email;
}
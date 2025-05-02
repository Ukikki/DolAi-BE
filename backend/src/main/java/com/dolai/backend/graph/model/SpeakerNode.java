package com.example.demo.graph.model;

import com.arangodb.springframework.annotation.ArangoId;
import com.arangodb.springframework.annotation.Document;
import org.springframework.data.annotation.Id;
import lombok.*;

@Document("speakers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SpeakerNode {

    @Id
    private String id;

    private String speakerName;   // ex: "Kim Minji"
    private String department;    // 선택: "Marketing"
    private String role;          // 선택: "Manager"

    public SpeakerNode(String speakerKey, String speakerName) {
        this.id = speakerKey;
        this.speakerName = speakerName;
    }
}
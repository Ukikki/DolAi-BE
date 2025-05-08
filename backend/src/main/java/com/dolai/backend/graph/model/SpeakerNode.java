package com.dolai.backend.graph.model;

import lombok.*;
import org.springframework.data.annotation.Id;

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
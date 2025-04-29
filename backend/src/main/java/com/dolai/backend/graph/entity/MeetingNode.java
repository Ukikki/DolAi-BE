package com.dolai.backend.graph.entity;

import com.arangodb.springframework.annotation.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 회의(미팅)를 나타내는 Node
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document("meeting_nodes")
public class MeetingNode {
    @Id
    private String id;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> participantIds;  // participant_nodes/{id}
}
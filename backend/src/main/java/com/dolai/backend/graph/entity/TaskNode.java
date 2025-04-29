package com.dolai.backend.graph.entity;

import com.arangodb.springframework.annotation.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import java.time.LocalDateTime;

/**
 * 회의에서 발생한 할 일(Task) Node
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document("task_nodes")
public class TaskNode {
    @Id
    private String id;
    private String description;
    private String assignedTo;  // Participant ID
    private LocalDateTime dueDate;
}
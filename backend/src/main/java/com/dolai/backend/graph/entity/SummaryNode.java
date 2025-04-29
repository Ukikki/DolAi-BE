package com.dolai.backend.graph.entity;

import com.arangodb.springframework.annotation.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import java.time.LocalDateTime;

/**
 * 회의 요약 Summary Node
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document("summary_nodes")
public class SummaryNode {
    @Id
    private String id;
    private String text;
    private LocalDateTime createdAt;
    private String sourceMeetingId;
}
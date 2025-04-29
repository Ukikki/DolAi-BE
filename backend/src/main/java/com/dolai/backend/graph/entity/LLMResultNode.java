package com.dolai.backend.graph.entity;

import com.arangodb.springframework.annotation.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import java.time.LocalDateTime;

/**
 * LLM 요청-응답 결과 저장용 Node (선택)
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document("llm_result_nodes")
public class LLMResultNode {
    @Id
    private String id;
    private String prompt;
    private String response;
    private LocalDateTime createdAt;
    private String meetingId;
}
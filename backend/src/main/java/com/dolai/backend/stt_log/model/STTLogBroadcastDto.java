//서버 → 프론트로 실시간 전송(WebSocket)
package com.dolai.backend.stt_log.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class STTLogBroadcastDto {
    private String speaker;
    private String text;
    private String textKo;
    private String textEn;
    private String textZh;
    private LocalDateTime timestamp;
}
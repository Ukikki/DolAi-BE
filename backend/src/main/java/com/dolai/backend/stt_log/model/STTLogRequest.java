//클라이언트 → 서버로 로그를 저장할 때 사용하는 요청용 DTO
package com.dolai.backend.stt_log.model;

import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class STTLogRequest {
    private String meetingId;
    private String speaker;
    private String text;
    private String textKo;
    private String textEn;
    private String textZh;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}

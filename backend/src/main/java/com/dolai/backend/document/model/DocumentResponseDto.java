package com.dolai.backend.document.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponseDto {
    private Long id;
    private Long meetingId;
    private String title;
    private String summary;
    private String fileUrl;
}

package com.dolai.backend.stt_log.model;

import com.dolai.backend.meeting.model.Meeting;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "stt_logs")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class STTLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;
    private String speakerName;
    private String text;

    @Column(name = "text_ko")
    private String textKo;

    @Column(name = "text_en")
    private String textEn;

    @Column(name = "text_zh")
    private String textZh;
    private LocalDateTime timestamp;

    @Builder.Default
    @Column(name = "synced")
    private Boolean synced = false; // 기본값: ArangoDB에 데이터를 옮긴 후 true로 바꿔 중복 저장 방지

    @Builder.Default
    @Column(name = "todo_checked")
    private Boolean todoChecked = false;
}
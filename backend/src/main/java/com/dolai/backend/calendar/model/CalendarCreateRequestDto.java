package com.dolai.backend.calendar.model;
import lombok.Data;
import java.util.List;

@Data
public class CalendarCreateRequestDto {
    private String title;          // 새로운 일정 제목
    private String startTime;       // "14:44" 이런 포맷 (프론트가 24시간 변환해서 줌)
    private List<String> participants;  // 초대할 사람 이메일 리스트
}
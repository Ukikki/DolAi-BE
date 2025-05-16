package com.dolai.backend.calendar.model;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CalendarCreateRequestDto {
    private String title;
    private String startDateTime; // "2025-05-15T14:44" → LocalDateTime.parse() 가능
    private List<String> participants;
}
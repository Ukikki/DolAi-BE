package com.dolai.backend.calendar.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class MonthlyCalendarDto {
    private int year;
    private int month;
    private List<DayCount> dates;
}

package com.dolai.backend.calendar.controller;

import com.dolai.backend.calendar.model.CalendarDto;
import com.dolai.backend.calendar.model.MonthlyCalendarDto;
import com.dolai.backend.calendar.service.CalendarService;
import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {
    private final CalendarService calendarService;

    //특정 날짜의 미팅 예약 리스트
    @GetMapping("/day/{date}")
    public ResponseEntity<?> getMeetingsByDate(
            @PathVariable("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal User user
    ) {
        List<CalendarDto> meetings = calendarService.getMeetingsByDate(date, user.getId());
        Map<String, Object> data = Map.of("date", date.toString(), "meetings", meetings);

        return ResponseEntity.ok(new SuccessDataResponse<>(data));
    }

    //해당 원에 미팅 기록이 있는지 (초록 점을 표시하기 위함)
    @GetMapping("/{year}/{month}")
    public ResponseEntity<?> getMonthlyMeetingDays(
            @PathVariable int year,
            @PathVariable int month,
            @AuthenticationPrincipal User user
    ) {
        try {
            LocalDate.of(year, month, 1);
        } catch (DateTimeException e) {
            return ResponseEntity.badRequest().body(new CustomException(ErrorCode.INVALID_REQUEST_STATUS));
        }

        MonthlyCalendarDto dto = calendarService.getMonthlyMeetingDays(year, month, user.getId());
        return ResponseEntity.ok(new SuccessDataResponse<>(dto));
    }

}

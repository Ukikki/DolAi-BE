package com.dolai.backend.calendar.service;

import com.dolai.backend.calendar.model.CalendarDto;
import com.dolai.backend.calendar.model.DayCount;
import com.dolai.backend.calendar.model.MonthlyCalendarDto;
import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final MeetingRepository meetingRepository;

    public List<CalendarDto> getMeetingsByDate(LocalDate date, String userId) {

        List<Meeting> hostMeetings = meetingRepository.findHostMeetingsByDate(date, userId);
        List<Meeting> participantMeetings = meetingRepository.findParticipantMeetingsByDate(date, userId);

        Set<Meeting> allMeetings = new HashSet<>();
        allMeetings.addAll(hostMeetings);
        allMeetings.addAll(participantMeetings);

        return allMeetings.stream()
                .map(CalendarDto::from)
                .collect(Collectors.toList());
    }

    public MonthlyCalendarDto getMonthlyMeetingDays(int year, int month, String userId) {
        List<Object[]> raw = meetingRepository.countMeetingsByDay(year, month, userId);
        List<DayCount> days = raw.stream()
                .map(row -> new DayCount(((Number) row[0]).intValue(), ((Number) row[1]).longValue()))
                .toList();

        return new MonthlyCalendarDto(year, month, days);
    }

}

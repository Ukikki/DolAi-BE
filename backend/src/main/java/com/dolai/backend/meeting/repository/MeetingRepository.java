package com.dolai.backend.meeting.repository;

import com.dolai.backend.meeting.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, String> {
    Optional<Meeting> findByInviteUrl(String inviteUrl);

    @Query("""
            SELECT m FROM Meeting m
            WHERE DATE(m.startTime) = :date
            AND m.hostUserId = :userId
            """)
    List<Meeting> findHostMeetingsByDate(@Param("date") LocalDate date, @Param("userId") String userId);

    @Query("""
            SELECT m FROM Meeting m
            JOIN Participant p ON p.meeting.id = m.id
            WHERE DATE(m.startTime) = :date
            AND p.user.id = :userId
            """)
    List<Meeting> findParticipantMeetingsByDate(@Param("date") LocalDate date, @Param("userId") String userId);

    @Query("""
            SELECT DAY(m.startTime) AS day, COUNT(m.id) AS count
            FROM Meeting m
            LEFT JOIN m.participants p
            WHERE YEAR(m.startTime) = :year
              AND MONTH(m.startTime) = :month
              AND (m.hostUserId = :userId OR p.user.id = :userId)
            GROUP BY DAY(m.startTime)
            """)
    List<Object[]> countMeetingsByDay(@Param("year") int year,
                                      @Param("month") int month,
                                      @Param("userId") String userId);

}

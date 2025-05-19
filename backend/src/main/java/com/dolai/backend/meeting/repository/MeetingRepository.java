package com.dolai.backend.meeting.repository;

import com.dolai.backend.meeting.model.Meeting;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, String> {
    Optional<Meeting> findByInviteUrl(String inviteUrl);

    @Query("""
    SELECT m FROM Meeting m
    JOIN m.participants p
    WHERE p.user.id = :userId AND DATE(m.startTime) = :date
""")
    List<Meeting> findMeetingsByParticipant(@Param("date") LocalDate date, @Param("userId") String userId);

    @Query("""
SELECT DAY(m.startTime), COUNT(m)
FROM Meeting m
JOIN m.participants p
WHERE p.user.id = :userId
  AND YEAR(m.startTime) = :year
  AND MONTH(m.startTime) = :month
GROUP BY DAY(m.startTime)
""")
    List<Object[]> countMeetingsByDay(@Param("year") int year, @Param("month") int month, @Param("userId") String userId);

    @Query("""
    select p.meeting from Participant p
    where p.user.id = :userId
      and p.meeting.status = 'ENDED'
    order by p.meeting.startTime desc
""")
    List<Meeting> findTopEndedMeetingsByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("""
    select p.meeting from Participant p
    where p.user.id = :userId
      and p.meeting.status = 'ENDED'
    order by p.meeting.startTime desc
    """)
    List<Meeting> findAllEndedMeetingsByUserId(@Param("userId") String userId);

}

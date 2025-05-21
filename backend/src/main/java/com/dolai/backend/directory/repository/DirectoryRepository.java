package com.dolai.backend.directory.repository;

import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.dolai.backend.directory.model.Directory;

import java.util.List;
import java.util.Optional;

@Repository
public interface DirectoryRepository extends JpaRepository<Directory, Long> {
    // 중복 이름 체크
    boolean existsByMeetingIdAndParentAndName(String meetingId, Directory parent, String name);

    // 하위 디렉토리 조회용
    List<Directory> findByParent(Directory parent);
    Optional<Directory> findByMeeting(Meeting meeting);
    Optional<Directory> findByMeetingId(String meetingId);
    Optional<Directory> findFirstByMeetingIdOrderByCreatedAtDesc(String meetingId);

}
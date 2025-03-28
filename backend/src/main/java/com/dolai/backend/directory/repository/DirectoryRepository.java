package com.dolai.backend.directory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.dolai.backend.directory.model.Directory;

import java.util.List;

@Repository
public interface DirectoryRepository extends JpaRepository<Directory, Long> {
    // 중복 이름 체크
    boolean existsByMeetingIdAndParentAndName(String meetingId, Directory parent, String name);

    // 하위 디렉토리 조회용
    List<Directory> findByParent(Directory parent);

}
package com.dolai.backend.document.repository;



import com.dolai.backend.document.model.Document;
import com.dolai.backend.meeting.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByMeeting(Meeting meeting);  // ✅ 메서드 정의
}

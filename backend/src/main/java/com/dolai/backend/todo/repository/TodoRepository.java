package com.dolai.backend.todo.repository;

import com.dolai.backend.todo.model.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByMeetingId(String meetingId);
    List<Todo> findByUserId(String userId);
}

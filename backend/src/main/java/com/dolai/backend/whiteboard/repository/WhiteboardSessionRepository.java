package com.dolai.backend.whiteboard.repository;

import com.dolai.backend.whiteboard.model.WhiteboardSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhiteboardSessionRepository extends JpaRepository<WhiteboardSession, String> {
}
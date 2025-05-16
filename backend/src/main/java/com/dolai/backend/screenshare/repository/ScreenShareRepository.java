package com.dolai.backend.screenshare.repository;

import com.dolai.backend.screenshare.model.ScreenShare;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreenShareRepository extends JpaRepository<ScreenShare, String> {
}

package com.dolai.backend.directory.service;

import com.dolai.backend.directory.model.DirectoryUser;
import com.dolai.backend.directory.model.enums.DirectoryColor;
import com.dolai.backend.directory.repository.DirectoryUserRepository;

public class DirectoryUserService {

    private DirectoryUserRepository directoryUserRepository;

    public void updateColor(Long directoryId, String userId, String newColor) {
        DirectoryUser du = directoryUserRepository
                .findByDirectory_IdAndUser_Id(directoryId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 디렉토리 권한 없음"));

        du.setColor(DirectoryColor.valueOf(newColor.toUpperCase())); // 예: \"blue\" -> BLUE
        directoryUserRepository.save(du);
    }
}

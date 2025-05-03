package com.dolai.backend.directory.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.directory.model.DirectoryUser;
import com.dolai.backend.directory.model.enums.DirectoryColor;
import com.dolai.backend.directory.repository.DirectoryUserRepository;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DirectoryUserService {

    private final DirectoryUserRepository directoryUserRepository;

    public void updateColor(Long directoryId, String userId, String newColor) {
        DirectoryUser du = directoryUserRepository
                .findByDirectoryIdAndUserId(directoryId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.DIRECTORY_USER_NOT_FOUND));

        du.setColor(DirectoryColor.valueOf(newColor.toUpperCase()));
        directoryUserRepository.save(du);
    }

    @Transactional
    public void updateDirectoryName(Long directoryId, String newName, User user) {
        DirectoryUser du = directoryUserRepository.findByDirectoryIdAndUserId(directoryId, user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.DIRECTORY_USER_NOT_FOUND));

        du.setName(newName);
        directoryUserRepository.save(du);
    }
}

package com.dolai.backend.directory.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.directory.model.*;
import com.dolai.backend.directory.model.enums.DirectoryColor;
import com.dolai.backend.directory.model.enums.DirectoryType;
import com.dolai.backend.directory.repository.DirectoryRepository;
import com.dolai.backend.directory.repository.DirectoryUserRepository;
import com.dolai.backend.document.repository.DocumentPlacementRepository;
import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.meeting.model.Participant;
import com.dolai.backend.meeting.repository.MeetingRepository;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectoryService {

    private final DirectoryRepository directoryRepository;
    private final DirectoryUserRepository directoryUserRepository;
    private final MeetingRepository meetingRepository;
    private final DocumentPlacementRepository documentPlacementRepository;

    @Transactional
    public DirectoryResponseDto createDirectory(DirectoryRequestDto request, User user) {

        DirectoryType type = DirectoryType.valueOf(request.getType().toUpperCase());
        if (type == DirectoryType.PERSONAL && request.getMeetingId() != null) {
            throw new CustomException(ErrorCode.MEETING_ID_SHOULD_BE_NULL);
        }

        if (type == DirectoryType.SHARED && request.getMeetingId() == null) {
            throw new CustomException(ErrorCode.MEETING_ID_REQUIRED);
        }

        // 부모 디렉터리 조회
        Directory parent = null;
        if (request.getParentDirectoryId() != null) {
            parent = directoryRepository.findById(request.getParentDirectoryId())
                    .orElseThrow(() -> new CustomException(ErrorCode.DIRECTORY_NOT_FOUND));
        }

        // 디렉터리 이름 중복 자동 처리 ("폴더", "폴더 (1)", "폴더 (2)" ...)
        String finalName = generateUniqueName(request.getName(), type, parent, user.getId(), request.getMeetingId());

        Directory directory = new Directory();
        directory.setName(finalName);
        directory.setParent(parent);
        directory.setType(type);

        // PERSONAL일 경우 user 설정 / SHARED일 경우 meeting 설정
        if (type == DirectoryType.PERSONAL) {
            directory.setUser(user);
            directory.setMeeting(null);
        } else {
            if (request.getMeetingId() == null) {
                throw new CustomException(ErrorCode.MEETING_ID_REQUIRED);
            }
            Meeting meeting = meetingRepository.findById(request.getMeetingId())
                    .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));
            directory.setUser(null);
            directory.setMeeting(meeting);
        }

        directoryRepository.save(directory);

        // DirectoryUser 생성
        if (type == DirectoryType.PERSONAL) {
            DirectoryUser du = DirectoryUser.builder()
                    .user(user)
                    .directory(directory)
                    .name(finalName)
                    .color(DirectoryColor.BLUE)
                    .build();
            directoryUserRepository.save(du);

        } else {
            // 공유 폴더는 해당 회의의 참가자 모두에게 DirectoryUser 생성
            Meeting meeting = meetingRepository.findById(request.getMeetingId())
                    .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));
            for (Participant participant : meeting.getParticipants()) {
                User participantUser = participant.getUser();
                DirectoryUser du = DirectoryUser.builder()
                        .user(participantUser)
                        .directory(directory)
                        .name(finalName)
                        .color(DirectoryColor.BLUE)
                        .build();
                directoryUserRepository.save(du);
            }
        }

        return DirectoryResponseDto.builder()
                .status("success")
                .message("Directory created successfully")
                .directoryId(directory.getId())
                .name(finalName)
                .build();
    }

    private String generateUniqueName(String baseName, DirectoryType type, Directory parent, String userId, String meetingId) {
        String name = baseName;
        int count = 1;
        boolean exists;

        do {
            if (type == DirectoryType.PERSONAL) {
                exists = directoryUserRepository.existsByUserIdAndDirectoryParentAndName(userId, parent, name);
            } else {
                exists = directoryRepository.existsByMeetingIdAndParentAndName(meetingId, parent, name);
            }

            if (exists) {
                name = baseName + " (" + count++ + ")";
            }

        } while (exists);
        return name;
    }

    @Transactional(readOnly = true)
    public List<DirectoryListResponseDto> getMyDirectories(User user) {
        List<DirectoryUser> directoryUsers = directoryUserRepository.findByUserAndDirectoryParentIsNull(user);
        return directoryUsers.stream()
                .map(du -> {
                    Directory dir = du.getDirectory();
                    return DirectoryListResponseDto.builder()
                            .directoryId(dir.getId())
                            .name(du.getName())
                            .parentDirectoryId(dir.getParent() != null ? dir.getParent().getId() : null)
                            .color(du.getColor().name()) // ← 색상 추가
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DirectoryListResponseDto> getChildDirectories(Long parentDirectoryId, User user) {
        Directory parent = null;

        if (parentDirectoryId != null) {
            parent = directoryRepository.findById(parentDirectoryId)
                    .orElseThrow(() -> new CustomException(ErrorCode.DIRECTORY_NOT_FOUND));
        }

        List<DirectoryUser> childDirectoryUsers = directoryUserRepository.findByUserAndDirectory_Parent(user, parent);

        return childDirectoryUsers.stream()
                .map(du -> DirectoryListResponseDto.builder()
                        .directoryId(du.getDirectory().getId())
                        .name(du.getName())
                        .color(du.getColor().name())
                        .parentDirectoryId(
                                du.getDirectory().getParent() != null
                                        ? du.getDirectory().getParent().getId()
                                        : null)
                        .build())
                .toList();
    }

    @Transactional
    public void deleteDirectory(Long directoryId, User user) {
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DIRECTORY_NOT_FOUND));

        documentPlacementRepository.deleteByDirectory(directory);
        directoryRepository.delete(directory);
    }
}
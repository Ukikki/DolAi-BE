package com.dolai.backend.directory.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.directory.model.*;
import com.dolai.backend.directory.model.enums.DirectoryColor;
import com.dolai.backend.directory.model.enums.DirectoryType;
import com.dolai.backend.directory.repository.DirectoryRepository;
import com.dolai.backend.directory.repository.DirectoryUserRepository;
import com.dolai.backend.document.model.Document;
import com.dolai.backend.document.model.DocumentPlacement;
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
        // 명세 위반 검사 추가
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

        // 디렉터리 이름 중복 자동 처리
        String finalName = generateUniqueName(request.getName(), type, parent, user.getId(), request.getMeetingId());

        Directory directory = new Directory();
        directory.setName(finalName);
        directory.setParent(parent);
        directory.setType(type);

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
    public List<DirectoryListResponseDto> getChildDirectories(String parentDirectoryId) {
        Directory parent = null;

        if (parentDirectoryId != null) {
            parent = directoryRepository.findById(Long.parseLong(parentDirectoryId))
                    .orElseThrow(() -> new CustomException(ErrorCode.DIRECTORY_NOT_FOUND));
        }

        List<Directory> children = directoryRepository.findByParent(parent);

        return children.stream()
                .map(dir -> DirectoryListResponseDto.builder()
                        .directoryId(dir.getId())
                        .name(dir.getName())
                        .parentDirectoryId(dir.getParent() != null ? String.valueOf(dir.getParent().getId()) : null)
                        .build())
                .toList();
    }

    @Transactional
    public void deleteDirectory(Long directoryId, User user) {
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DIRECTORY_NOT_FOUND));

        //해당 디렉터리에 연결된 문서 배치 삭제
        documentPlacementRepository.deleteByDirectory(directory);

        // 2. directory_user 등 관련 정보 삭제 (cascade 또는 수동)

        //디렉터리 자체 삭제
        directoryRepository.delete(directory);
    }
}

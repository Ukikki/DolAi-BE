package com.dolai.backend.directory.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.directory.model.*;
import com.dolai.backend.directory.model.enums.DirectoryColor;
import com.dolai.backend.directory.model.enums.DirectoryType;
import com.dolai.backend.directory.repository.DirectoryRepository;
import com.dolai.backend.directory.repository.DirectoryUserRepository;
import com.dolai.backend.document.repository.DocumentPlacementRepository;
import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.meeting.model.Participant;
import com.dolai.backend.meeting.repository.MeetingRepository;
import com.dolai.backend.metadata.service.DirectoryMetaDataService;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    private final DirectoryMetaDataService directoryMetaDataService;

    @Transactional
    public DirectoryResponseDto createDirectory(DirectoryRequestDto request, User user) {
        Directory dir = createDirectoryCore(request, user);
        DirectoryResponseDto response = DirectoryResponseDto.builder()
                .directoryId(dir.getId())
                .name(dir.getName())
                .build();
        return response;
    }

    public Directory createDirectoryCore(DirectoryRequestDto request, User user) {
        DirectoryType type = DirectoryType.valueOf(request.getType().toUpperCase());

        if (type == DirectoryType.PERSONAL && request.getMeetingId() != null)
            throw new CustomException(ErrorCode.MEETING_ID_SHOULD_BE_NULL);
        if (type == DirectoryType.SHARED && request.getMeetingId() == null)
            throw new CustomException(ErrorCode.MEETING_ID_REQUIRED);

        Directory parent = null;
        if (request.getParentDirectoryId() != null) {
            parent = directoryRepository.findById(request.getParentDirectoryId())
                    .orElseThrow(() -> new CustomException(ErrorCode.DIRECTORY_NOT_FOUND));
        }

        String finalName = generateUniqueName(request.getName(), type, parent, user.getId(), request.getMeetingId());

        Directory directory = new Directory();
        directory.setName(finalName);
        directory.setParent(parent);
        directory.setType(type);

        if (type == DirectoryType.PERSONAL) {
            directory.setUser(user);
        } else {
            Meeting meeting = meetingRepository.findById(request.getMeetingId())
                    .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));
            directory.setMeeting(meeting);
        }

        directoryRepository.save(directory);
        createDirectoryUsers(directory, user, request.getMeetingId(), finalName, type);

        return directory;
    }

    private void createDirectoryUsers(Directory directory, User user, String meetingId, String name, DirectoryType type) {
        if (type == DirectoryType.PERSONAL) {
            directoryUserRepository.save(DirectoryUser.builder()
                    .user(user)
                    .directory(directory)
                    .name(name)
                    .color(DirectoryColor.BLUE)
                    .build());
        } else {
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));
            for (Participant participant : meeting.getParticipants()) {
                directoryUserRepository.save(DirectoryUser.builder()
                        .user(participant.getUser())
                        .directory(directory)
                        .name(name)
                        .color(DirectoryColor.BLUE)
                        .build());
            }
        }
    }

    //회의 종료 후, 공유 문서 생성
    @Transactional
    public Directory createSharedDirectory(Meeting meeting, User user) {
        String name = meeting.getStartTime().toLocalDate().toString();
        DirectoryRequestDto dto = new DirectoryRequestDto(name, null, "SHARED", meeting.getId());
        Directory directory = createDirectoryCore(dto, user);
        directoryMetaDataService.createMetadataFor(directory);
        return directory;
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

    @Transactional(readOnly = true)
    public DirectoryResponseDto getDirectoryByMeetingId(String meetingId) {
        // 회의 존재 확인
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));

        // 회의와 연결된 디렉토리 조회
        Directory directory = directoryRepository.findByMeeting(meeting)
                .orElseThrow(() -> new CustomException(ErrorCode.DIRECTORY_NOT_FOUND));

        return DirectoryResponseDto.builder()
                .directoryId(directory.getId())
                .name(directory.getName())
                .build();
    }
}
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

    // 개인 디렉터리 생성
    @Transactional
    public DirectoryResponseDto createDirectory(DirectoryRequestDto request, User user) {
        Directory dir = createDirectoryCore(request, user);
        DirectoryResponseDto response = DirectoryResponseDto.builder()
                .directoryId(dir.getId())
                .name(dir.getName())
                .build();
        return response;
    }

    // 개인 디렉터리 생성
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

        String directoryName = request.getName();

        Directory directory = new Directory();
        directory.setName(directoryName);
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
        createDirectoryUsers(directory, user, request.getMeetingId(), directoryName, type);

        return directory;
    }

    private void createDirectoryUsers(Directory directory, User user, String meetingId, String baseName, DirectoryType type) {
        if (type == DirectoryType.PERSONAL) {
            String userName = generateUserUniqueName(user, baseName);
            directoryUserRepository.save(
                    DirectoryUser.builder()
                            .user(user)
                            .directory(directory)
                            .name(userName)
                            .color(DirectoryColor.BLUE)
                            .build()
            );
        } else {
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));
            for (Participant participant : meeting.getParticipants()) {
                User participantUser = participant.getUser();
                String userName = generateUserUniqueName(participantUser, baseName);
                directoryUserRepository.save(
                        DirectoryUser.builder()
                                .user(participantUser)
                                .directory(directory)
                                .name(userName)
                                .color(DirectoryColor.BLUE)
                                .build()
                );
            }
        }
    }

    //회의 종료 후, 공유 문서 생성
    @Transactional
    public Directory createSharedDirectory(Meeting meeting, User user) {
        String name = meeting.getTitle(); // 미팅 제목으로 설정
        DirectoryRequestDto dto = new DirectoryRequestDto(name, null, "SHARED", meeting.getId());
        Directory directory = createDirectoryCore(dto, user);
        directoryMetaDataService.createMetadataFor(directory);
        return directory;
    }

    private String generateUserUniqueName(User user, String baseName) {
        String name = baseName;
        int count = 1;
        boolean exists;

        do {
            // 사용자에게 이미 같은 이름의 디렉터리가 있는지 확인
            exists = directoryUserRepository.existsByUserAndName(user, name);
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
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));
        Directory directory = directoryRepository.findByMeeting(meeting)
                .orElseThrow(() -> new CustomException(ErrorCode.DIRECTORY_NOT_FOUND));

        return DirectoryResponseDto.builder()
                .directoryId(directory.getId())
                .name(directory.getName())
                .build();
    }
}
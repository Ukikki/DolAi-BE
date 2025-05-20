    package com.dolai.backend.metadata.service;

    import com.dolai.backend.common.exception.CustomException;
    import com.dolai.backend.common.exception.ErrorCode;
    import com.dolai.backend.directory.model.Directory;
    import com.dolai.backend.directory.model.enums.DirectoryType;
    import com.dolai.backend.metadata.model.DirectoryMetaData;
    import com.dolai.backend.metadata.model.DirectoryMetaDataResponseDto;
    import com.dolai.backend.metadata.repository.DirectoryMetaDataRepository;
    import com.dolai.backend.meeting.model.Meeting;
    import com.dolai.backend.user.model.User;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;

    import java.util.List;

    @Service
    @RequiredArgsConstructor
    public class DirectoryMetaDataService {

        private final DirectoryMetaDataRepository directoryMetaDataRepository;

        public void createMetadataFor(Directory directory) {
            DirectoryMetaData meta = DirectoryMetaData.of(directory);
            directoryMetaDataRepository.save(meta);
        }

        public DirectoryMetaDataResponseDto getMetaDataByDirectoryId(Long directoryId, User user) {
            DirectoryMetaData metaData = directoryMetaDataRepository.findByDirectoryId(directoryId)
                    .orElseThrow(() -> new CustomException(ErrorCode.METADATA_NOT_FOUND));

            Directory directory = metaData.getDirectory();
            DirectoryType type = directory.getType(); // 💡 여기서 직접 가져옴

            // PERSONAL 디렉터리는 본인만 접근 가능
            if (type == DirectoryType.PERSONAL) {
                if (directory.getUser() == null || !directory.getUser().getId().equals(user.getId())) {
                    throw new CustomException(ErrorCode.FORBIDDEN);
                }
            }

            // SHARED 디렉터리는 미팅 참가자만 접근 가능
            else if (type == DirectoryType.SHARED) {
                Meeting meeting = directory.getMeeting();
                boolean isParticipant = meeting.getParticipants().stream()
                        .anyMatch(p -> p.getUser().getId().equals(user.getId()));

                if (!isParticipant) {
                    throw new CustomException(ErrorCode.FORBIDDEN);
                }
            }

            // DTO 변환
            String meetingTitle = null;
            List<DirectoryMetaDataResponseDto.ParticipantInfo> participants = List.of();

            if (type == DirectoryType.SHARED && directory.getMeeting() != null) {
                meetingTitle = directory.getMeeting().getTitle();
                participants = directory.getMeeting().getParticipants().stream()
                        .map(p -> DirectoryMetaDataResponseDto.ParticipantInfo.builder()
                                .name(p.getUser().getName())
                                .email(p.getUser().getEmail())
                                .build())
                        .toList();
            }

            return DirectoryMetaDataResponseDto.builder()
                    .type(type.name())
                    .size(formatFileSize(metaData.getSize()))
                    .meetingTitle(meetingTitle)
                    .participants(participants)
                    .createdAt(metaData.getCreatedAt())
                    .updatedAt(metaData.getUpdatedAt())
                    .build();
        }

        // size 포맷팅
        private String formatFileSize(Long sizeInBytes) {
            if (sizeInBytes == null) return "0 B";
            if (sizeInBytes < 1024) {
                return sizeInBytes + " B";
            } else if (sizeInBytes < 1024 * 1024) {
                return String.format("%.1f KB", sizeInBytes / 1024.0);
            } else if (sizeInBytes < 1024 * 1024 * 1024) {
                return String.format("%.1f MB", sizeInBytes / (1024.0 * 1024.0));
            } else {
                return String.format("%.1f GB", sizeInBytes / (1024.0 * 1024.0 * 1024.0));
            }
        }
    }

package com.dolai.backend.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@Slf4j
@Service
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    public S3Service(
            @Value("${aws.accessKeyId}") String accessKeyId,
            @Value("${aws.secretKey}") String secretKey,
            @Value("${aws.s3.bucket}") String bucketName,
            @Value("${aws.region}") String region) {

        this.bucketName = bucketName;

        // AWS 자격 증명 설정
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKeyId, secretKey);

        // S3 클라이언트 생성
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();

        log.info("S3 업로드 서비스 초기화 완료: 버킷={}, 리전={}", bucketName, region);
    }

    /**
     * 회의록 파일을 S3에 업로드
     * @param file 업로드할 파일
     * @param meetingId 회의 ID
     * @return 업로드된 파일의 URL
     */
    public String uploadMeetingDocument(File file, String meetingId, String fileName) {
        try {
            // 현재 시간을 포함한 파일 키 생성 (폴더 구조 포함)
            String fileKey = String.format("meetings/%s/%s", meetingId, fileName);

            // S3에 파일 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));

            log.info("✅ 회의록 업로드 완료: meetingId={}, 파일명={}", meetingId, file.getName());

            // S3 URL 생성 및 반환
            String fileUrl = String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileKey);
            return fileUrl;

        } catch (Exception e) {
            log.error("❌ S3 업로드 실패: meetingId={}, 파일명={}", meetingId, file.getName(), e);
            throw new RuntimeException("S3 업로드 중 오류 발생", e);
        }
    }

    public Long getFileSize(String fileUrl) {
        try {
            // URL에서 키(파일 경로) 추출
            String key = extractKeyFromUrl(fileUrl);

            // HeadObject 요청 생성
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // 객체 메타데이터 조회
            HeadObjectResponse response = s3Client.headObject(headObjectRequest);

            // 파일 크기 반환
            Long fileSize = response.contentLength();
            log.info("✅ 파일 크기 조회 성공: key={}, size={} bytes", key, fileSize);

            return fileSize;
        } catch (S3Exception e) {
            log.error("❌ S3 파일 크기 조회 실패: {}", e.getMessage(), e);
            return 0L;  // 오류 발생 시 기본값 반환
        }
    }

    private String extractKeyFromUrl(String fileUrl) {
        try {
            String key;

            // URL에서 버킷 이름 이후의 경로 추출
            if (fileUrl.contains("/" + bucketName + "/")) {
                // 형식: https://s3.region.amazonaws.com/bucket-name/path/to/file.txt
                key = fileUrl.substring(fileUrl.indexOf("/" + bucketName + "/") + bucketName.length() + 2);
            } else {
                // 형식: https://bucket-name.s3.region.amazonaws.com/path/to/file.txt
                key = fileUrl.substring(fileUrl.indexOf(".com/") + 5);
            }

            // URL 인코딩 처리
            return key.replace("%20", " ");
        } catch (Exception e) {
            log.error("❌ S3 URL에서 키 추출 실패: {}", fileUrl, e);
            throw new IllegalArgumentException("Invalid S3 URL: " + fileUrl);
        }
    }


    public File downloadTempFile(String fileUrl) {
        try {
            // 1. 키 추출
            String key = extractKeyFromUrl(fileUrl);

            // 2. 임시 파일 생성
            File tempFile = File.createTempFile("docx-", ".docx");

            // 3. S3에서 파일 다운로드
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            try (InputStream inputStream = s3Client.getObject(getObjectRequest);
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                log.info("✅ S3 파일 다운로드 완료: {}", tempFile.getAbsolutePath());
                return tempFile;
            }

        } catch (Exception e) {
            log.error("❌ S3 파일 다운로드 실패: {}", fileUrl, e);
            throw new RuntimeException("S3 파일 다운로드 실패", e);
        }
    }
}
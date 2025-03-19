// 생성일, 수정일 자동 설정 기본 엔티티
package com.dolai.backend.common.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
    @CreatedDate
    private LocalDateTime createdAt;  // 데이터가 입력되는 시각

    @LastModifiedDate
    private LocalDateTime updatedAt;  // 데이터가 수정된 시각
}
package com.goggles.common.domain;

import com.querydsl.core.annotations.QuerySupertype;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 생성/수정 시각만 관리하는 기반 엔티티.
 *
 * <p>소비자 서비스에서 반드시 {@code @EnableJpaAuditing} 을 선언해야 합니다.
 *
 * <pre>{@code
 * @EnableJpaAuditing
 * @SpringBootApplication
 * public class MyServiceApplication { ... }
 * }</pre>
 */
@Getter
@QuerySupertype
@MappedSuperclass
@Access(AccessType.FIELD)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTime {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
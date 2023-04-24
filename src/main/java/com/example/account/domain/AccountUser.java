package com.example.account.domain;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDateTime;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
public class AccountUser {
    @Id
    @GeneratedValue
    private Long id; // 기본적인 PK를 공통으로 가져가면 사용하기 편리

    private String name; // 사용자 이름

    @CreatedDate
    private LocalDateTime createdAt; // 테이블 메타정보
    @LastModifiedDate
    private LocalDateTime updatedAt; // 테이블 메타정보
}

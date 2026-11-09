package com.likelion.notebook.auth.entity;

import com.likelion.notebook.global.common.BaseTimeEntity;
import com.likelion.notebook.user.entity.UserType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** 처음 로그인할 때 한 번 입력. 기수와 계정 유형을 정함 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InviteCode extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 6)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType userType;

    @Column(nullable = false)
    private Integer cohort;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private LocalDate expiresAt;

    @Column(nullable = false)
    private int usedCount;

    @Builder
    private InviteCode(String code, UserType userType, Integer cohort, LocalDate expiresAt) {
        this.code = code;
        this.userType = userType;
        this.cohort = cohort;
        this.expiresAt = expiresAt;
        this.enabled = true;
        this.usedCount = 0;
    }

    /** 꺼졌거나 만료됐으면 가입 불가 (데모에는 만료 체크가 빠져 있었음) */
    public boolean isUsable(LocalDate today) {
        return enabled && !today.isAfter(expiresAt);
    }

    public void toggle() {
        this.enabled = !this.enabled;
    }

    public void increaseUsed() {
        this.usedCount++;
    }
}

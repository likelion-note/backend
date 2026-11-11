package com.likelion.notebook.user.entity;

import com.likelion.notebook.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이름(name)은 운영진 멤버 목록에서만 내려줄 것.
 * 노트·검토·알림 응답 DTO에는 절대 포함하지 않기.
 *
 * part = 기본 파트. 운영진이 지정하고, 프로젝트에 배정할 때 ProjectAssignment.part로 복사됨.
 * 화면에 보이는 "지금 파트"는 항상 현재 ProjectAssignment.part를 쓰기.
 */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String googleSub; // 구글 계정 고유 id

    @Column(nullable = false, length = 30)
    private String name; // 이름 또는 닉네임

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType type;

    @Column(nullable = false)
    private Integer cohort; // 기수

    @Enumerated(EnumType.STRING)
    private Part part; // 기본 파트. 지정 전이면 null

    @Builder
    private User(String email, String googleSub, String name, UserType type, Integer cohort) {
        this.email = email;
        this.googleSub = googleSub;
        this.name = name;
        this.type = type;
        this.cohort = cohort;
        if (type == UserType.ADMIN) {
            this.part = Part.OPS;
        }
    }

    public boolean isAdmin() {
        return type == UserType.ADMIN;
    }

    /** 기본 파트 변경. 이미 배정된 프로젝트의 파트는 바뀌지 않음 */
    public void changeDefaultPart(Part part) {
        this.part = part;
    }
}

package com.likelion.notebook.user.entity;

import com.likelion.notebook.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이름(name)은 운영진 멤버 목록에서만 내려줄 것.
 * 노트·검토·알림 응답 DTO에는 절대 포함하지 않기.
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
    private Part part; // 배정 전이면 null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_project_id")
    private ProjectCategory currentProject; // 배정 전이면 null

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

    public boolean isAssigned() {
        return part != null && currentProject != null;
    }

    /** 배정 변경. 이전 일지는 이전 분류에 남음(일지가 project를 따로 들고 있음) */
    public void assign(Part part, ProjectCategory project) {
        this.part = part;
        this.currentProject = project;
    }
}

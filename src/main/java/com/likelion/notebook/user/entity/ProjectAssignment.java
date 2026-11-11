package com.likelion.notebook.user.entity;

import com.likelion.notebook.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배정 = 사람 × 프로젝트 분류 × 그 프로젝트에서의 파트.
 * 재배정해도 이전 행은 지우지 않고 current=false로 남김 (이력).
 * - "지금 배정"은 current=true인 행 (사람당 최대 1개)
 * - 같은 프로젝트 팀원에게 노트 숨김은 이 테이블 기준 (지난 프로젝트까지 숨길지는 팀에서 정하기)
 */
@Getter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "project_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectAssignment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private ProjectCategory project;

    @Enumerated(EnumType.STRING)
    private Part part; // 이 프로젝트에서의 파트. 운영(OPS) 분류는 OPS

    @Column(name = "is_current", nullable = false)
    private boolean current;

    public ProjectAssignment(User user, ProjectCategory project, Part part) {
        this.user = user;
        this.project = project;
        this.part = part;
        this.current = true;
    }

    /** 예전에 배정됐던 프로젝트로 다시 배정 */
    public void reactivate(Part part) {
        this.part = part;
        this.current = true;
    }

    public void changePart(Part part) {
        this.part = part;
    }

    public void end() {
        this.current = false;
    }
}

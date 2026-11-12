package com.likelion.notebook.user.entity;

import com.likelion.notebook.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배정 이력. 지금 프로젝트뿐 아니라 거쳐간 프로젝트도 남김.
 * "같은 프로젝트 팀원에게는 노트 숨김"을 계산할 때 이 테이블을 봄.
 * (지난 프로젝트 팀원에게도 숨길지는 팀에서 정하기. 명세서 3장 참고)
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

    public ProjectAssignment(User user, ProjectCategory project) {
        this.user = user;
        this.project = project;
    }
}

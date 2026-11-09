package com.likelion.notebook.user.entity;

import com.likelion.notebook.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 운영진이 만드는 프로젝트 분류. 예: "14기 해커톤 · 멋사수첩팀" */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectCategory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectKind kind;

    @Column(nullable = false)
    private Integer cohort;

    /** 지난 기수 노트에 "A팀"으로 보여줄 글자. 같은 기수 노트에는 절대 노출하지 않기 */
    @Column(length = 2)
    private String teamLetter;

    @Builder
    private ProjectCategory(String title, ProjectKind kind, Integer cohort, String teamLetter) {
        this.title = title;
        this.kind = kind;
        this.cohort = cohort;
        this.teamLetter = teamLetter;
    }

    public enum ProjectKind {
        TEAM, // 아기사자 팀
        OPS   // 운영
    }
}

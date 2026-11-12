package com.likelion.notebook.journal.entity;

import com.likelion.notebook.journal.entity.JournalEnums.PortfolioFormat;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 포트폴리오 요약 결과. 만들 때마다 한 행 (하루 5회 제한은 created_at으로 셈).
 * 수정하지 않으므로 updated_at 없음 → BaseTimeEntity를 상속하지 않음.
 */
@Getter
@Entity
@Table(name = "portfolio_summary")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioSummary {

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
    @Column(nullable = false, length = 30)
    private PortfolioFormat format;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String result;

    /** 근거 일지 id 배열 (JSON) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Long> sourceJournalIds = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private PortfolioSummary(User user, ProjectCategory project, PortfolioFormat format,
                             String result, List<Long> sourceJournalIds) {
        this.user = user;
        this.project = project;
        this.format = format;
        this.result = result;
        this.sourceJournalIds = new ArrayList<>(sourceJournalIds);
    }
}

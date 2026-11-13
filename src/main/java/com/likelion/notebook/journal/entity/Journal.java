package com.likelion.notebook.journal.entity;

import com.likelion.notebook.global.common.BaseTimeEntity;
import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.journal.entity.JournalEnums.*;
import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 성장일지 = 원문. 승인되면 AI 정리본(summary*) 부분이 "노트"로 게시판에 보임.
 * 노트를 따로 테이블로 두지 않고 이 엔티티의 status/scope로 공개 여부를 판단.
 * 기수는 작성자 기수(author.cohort)를 씀. 읽음·도움돼요 개수는 note_reaction에서 매번 셈.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Journal extends BaseTimeEntity {

    private static final int STUCK_MIN_LENGTH = 30;
    private static final int REJECT_REASON_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectCategory project; // null이면 "미분류"

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Part part; // 이때 내 파트

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Activity activity;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Timing timing;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Scope scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    /** 원문. 작성자 본인만 봄 */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "stuck", column = @Column(name = "raw_stuck", columnDefinition = "TEXT")),
            @AttributeOverride(name = "solved", column = @Column(name = "raw_solved", columnDefinition = "TEXT")),
            @AttributeOverride(name = "again", column = @Column(name = "raw_again", columnDefinition = "TEXT")),
            @AttributeOverride(name = "memo", column = @Column(name = "raw_memo", columnDefinition = "TEXT"))
    })
    private JournalBody raw;

    // ---- AI 정리 결과 (검토 요청 전에 채워짐) ----
    private String summaryTitle;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "stuck", column = @Column(name = "sum_stuck", columnDefinition = "TEXT")),
            @AttributeOverride(name = "solved", column = @Column(name = "sum_solved", columnDefinition = "TEXT")),
            @AttributeOverride(name = "again", column = @Column(name = "sum_again", columnDefinition = "TEXT")),
            @AttributeOverride(name = "memo", column = @Column(name = "sum_memo", columnDefinition = "TEXT"))
    })
    private JournalBody summary; // 이름 가린 정리본

    /** 상황 태그 최대 3개. JSON 문자열 배열로 저장 (예: ["CONFLICT","DELAY"]) */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Tag> tags = new ArrayList<>();

    // ---- 검토 ----
    @Column(length = REJECT_REASON_MAX_LENGTH)
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer; // 검토한 운영진

    private LocalDateTime reviewedAt; // 승인·반려 처리 시각

    @Builder
    private Journal(User author, ProjectCategory project, Part part, Activity activity,
                    Timing timing, Scope scope, JournalBody raw) {
        this.author = author;
        this.project = project;
        this.part = part;
        this.activity = activity;
        this.timing = timing;
        this.scope = scope;
        this.raw = raw;
        this.status = Status.DRAFT;
    }

    public boolean isWrittenBy(Long userId) {
        return author.getId().equals(userId);
    }

    /** 자동 저장 (임시 저장·돌려받음 상태에서만) */
    public void updateDraft(ProjectCategory project, Part part, Activity activity,
                            Timing timing, Scope scope, JournalBody raw) {
        if (status != Status.DRAFT && status != Status.REJECTED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "지금은 수정할 수 없어요");
        }
        this.project = project;
        this.part = part;
        this.activity = activity;
        this.timing = timing;
        this.scope = scope;
        this.raw = raw;
    }

    /** 한 번이라도 AI 정리를 했는지 (보완 질문은 처음 정리할 때만) */
    public boolean isSummarized() {
        return summaryTitle != null;
    }

    /** 4. AI 정리 전 확인: 상태 → 필수 조건(처음 걸린 것 하나만) */
    public void checkReadyForSummary() {
        if (status != Status.DRAFT && status != Status.REJECTED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "지금은 정리할 수 없어요");
        }
        if (activity == null || timing == null || scope == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "활동·시점·공개 범위를 골라 주세요");
        }
        if (raw == null || raw.getStuck() == null || raw.getStuck().strip().length() < STUCK_MIN_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "막힌 것을 30자 이상 써 주세요");
        }
        if (raw.getSolved() == null || raw.getSolved().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "넘긴 방법을 써 주세요");
        }
    }

    public void applySummary(String title, JournalBody summary, List<Tag> tags) {
        this.summaryTitle = title;
        this.summary = summary;
        this.tags = new ArrayList<>(tags);
    }

    /**
     * 5. 검토 요청 (처음 제출 + 돌려받은 뒤 다시 제출).
     * 태그는 AI가 준 것 중에서 빼기만 가능 (0개도 됨). 돌려받은 사유는 지움
     */
    public void submit(List<Tag> keptTags) {
        if (status != Status.DRAFT && status != Status.REJECTED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 검토 요청한 일지예요");
        }
        if (!isSummarized() || summary == null || scope == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "먼저 AI 정리를 해 주세요");
        }
        if (!tags.containsAll(keptTags)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "태그는 정리된 것 중에서만 고를 수 있어요");
        }
        if (raw == null || raw.getStuck() == null || raw.getStuck().strip().length() < STUCK_MIN_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "막힌 것을 30자 이상 써 주세요");
        }
        this.tags = tags.stream().filter(keptTags::contains).toList();
        this.status = Status.PENDING;
        this.rejectReason = null;
    }

    public void approve(User reviewer) {
        requirePending();
        this.status = Status.APPROVED;
        markReviewed(reviewer);
    }

    public void reject(User reviewer, String reason) {
        requirePending();
        if (reason == null || reason.isBlank() || reason.strip().length() > REJECT_REASON_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "사유를 200자 이내로 써 주세요");
        }
        this.status = Status.REJECTED;
        this.rejectReason = reason.strip();
        markReviewed(reviewer);
    }

    private void markReviewed(User reviewer) {
        this.reviewer = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }

    private void requirePending() {
        if (status != Status.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 처리된 일지예요");
        }
    }
}

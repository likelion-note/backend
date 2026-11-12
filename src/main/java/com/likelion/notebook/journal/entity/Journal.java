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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 성장일지 = 원문. 승인되면 AI 정리본(summary*) 부분이 "노트"로 게시판에 보임.
 * 노트를 따로 테이블로 두지 않고 이 엔티티의 status/scope로 공개 여부를 판단.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Journal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false)
    private Integer cohort; // 쓸 당시 기수

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectCategory project; // null이면 "미분류"

    @Enumerated(EnumType.STRING)
    private Part part;

    @Enumerated(EnumType.STRING)
    private Activity activity;

    @Enumerated(EnumType.STRING)
    private Timing timing;

    @Enumerated(EnumType.STRING)
    private Scope scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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

    @Enumerated(EnumType.STRING)
    private Category category;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "journal_tag", joinColumns = @JoinColumn(name = "journal_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tag")
    private Set<Tag> tags = new HashSet<>();

    private int maskedCount;

    // ---- 검토 ----
    @Column(length = 200)
    private String rejectReason;

    private LocalDateTime submittedAt; // 검토 대기열은 이 순서(오래된 순)

    // ---- 반응 (조회 성능용 카운터. 실제 기록은 note 패키지의 NoteRead/NoteHelp) ----
    private int readCount;
    private int helpCount;

    @Builder
    private Journal(User author, ProjectCategory project, Part part, Activity activity,
                    Timing timing, Scope scope, JournalBody raw) {
        this.author = author;
        this.cohort = author.getCohort();
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

    public void applySummary(String title, JournalBody summary, Category category, Set<Tag> tags, int maskedCount) {
        this.summaryTitle = title;
        this.summary = summary;
        this.category = category;
        this.tags = new HashSet<>(tags);
        this.maskedCount = maskedCount;
    }

    /** 검토 요청 (처음 제출 + 돌려받은 뒤 다시 제출) */
    public void submit() {
        if (status != Status.DRAFT && status != Status.REJECTED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 검토 요청한 일지예요");
        }
        if (raw == null || raw.getStuck() == null || raw.getStuck().strip().length() < 30) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "막힌 것을 30자 이상 써 주세요");
        }
        if (summary == null || scope == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "먼저 AI 정리를 해 주세요");
        }
        this.status = Status.PENDING;
        this.rejectReason = null;
        this.submittedAt = LocalDateTime.now();
    }

    public void approve() {
        requirePending();
        this.status = Status.APPROVED;
    }

    public void reject(String reason) {
        requirePending();
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "사유를 200자 이내로 써 주세요");
        }
        this.status = Status.REJECTED;
        this.rejectReason = reason.strip();
    }

    public void increaseRead() {
        this.readCount++;
    }

    public void increaseHelp() {
        this.helpCount++;
    }

    public void decreaseHelp() {
        if (helpCount > 0) this.helpCount--;
    }

    private void requirePending() {
        if (status != Status.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 처리된 일지예요");
        }
    }
}

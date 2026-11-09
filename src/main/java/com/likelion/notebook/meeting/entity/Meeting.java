package com.likelion.notebook.meeting.entity;

import com.likelion.notebook.global.common.BaseTimeEntity;
import com.likelion.notebook.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 회의 정리. 본인만 봄, 노트로 올라가지 않음. 성장일지 쓸 때 지난 7일 것을 불러옴 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingType type;

    @Column(nullable = false)
    private LocalDate meetingDate;

    /** 정한 것 (한 줄에 하나) */
    @ElementCollection
    @CollectionTable(name = "meeting_decision", joinColumns = @JoinColumn(name = "meeting_id"))
    @OrderColumn(name = "seq")
    @Column(name = "content", length = 300)
    private List<String> decisions = new ArrayList<>();

    /** 누가 무엇을 언제까지 */
    @ElementCollection
    @CollectionTable(name = "meeting_action", joinColumns = @JoinColumn(name = "meeting_id"))
    @OrderColumn(name = "seq")
    private List<MeetingAction> actions = new ArrayList<>();

    @Builder
    private Meeting(User owner, MeetingType type, LocalDate meetingDate,
                    List<String> decisions, List<MeetingAction> actions) {
        this.owner = owner;
        this.type = type;
        this.meetingDate = meetingDate;
        if (decisions != null) this.decisions.addAll(decisions);
        if (actions != null) this.actions.addAll(actions);
    }

    public enum MeetingType {
        FIRST,  // 첫 회의
        IDEA,   // 아이디어 회의
        CHECK,  // 중간 점검 회의
        RETRO   // 회고 회의
    }
}

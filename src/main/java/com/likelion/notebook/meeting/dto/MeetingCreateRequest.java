package com.likelion.notebook.meeting.dto;

import com.likelion.notebook.meeting.entity.Meeting.MeetingType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * SCR-MEET-03 회의 정리.
 * - meetingDate 생략 시 오늘
 * - decisions(정한 것)와 actions(누가·무엇·언제까지) 중 하나는 있어야 함 (빈 줄은 서버에서 버림)
 */
public record MeetingCreateRequest(
        @NotNull MeetingType type,
        LocalDate meetingDate,
        List<@Size(max = 300) String> decisions,
        List<@Valid ActionItem> actions
) {

    /** due는 요일 한 글자("월"~"일") 또는 짧은 표현. 비워도 됨 */
    public record ActionItem(@Size(max = 30) String who, @Size(max = 200) String what, @Size(max = 10) String due) {
    }
}

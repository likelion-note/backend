package com.likelion.notebook.meeting.dto;

import com.likelion.notebook.meeting.entity.Meeting;
import com.likelion.notebook.meeting.entity.Meeting.MeetingType;

import java.time.LocalDate;
import java.util.List;

/** shareText: 공유하기 버튼이 그대로 복사할 원문 (FN-MEET-04) */
public record MeetingResponse(
        Long id,
        MeetingType type,
        String typeLabel,
        LocalDate meetingDate,
        List<String> decisions,
        List<Action> actions,
        String shareText
) {

    public record Action(String who, String what, String due) {
    }

    public static MeetingResponse from(Meeting m) {
        List<Action> actions = m.getActions().stream()
                .map(a -> new Action(a.getWho(), a.getWhat(), a.getDue()))
                .toList();
        List<String> decisions = List.copyOf(m.getDecisions());
        return new MeetingResponse(m.getId(), m.getType(), MeetingLabels.typeLabel(m.getType()), m.getMeetingDate(),
                decisions, actions, shareText(m.getType(), m.getMeetingDate(), decisions, actions));
    }

    /**
     * 예)
     * 아이디어 회의 · 10/27
     *
     * 정한 것
     * - 주제는 동아리 일정 관리로
     *
     * 누가 언제까지
     * - 민수 · 경쟁 서비스 조사 (수)
     */
    static String shareText(MeetingType type, LocalDate date, List<String> decisions, List<Action> actions) {
        StringBuilder sb = new StringBuilder()
                .append(MeetingLabels.typeLabel(type)).append(" · ")
                .append(date.getMonthValue()).append('/').append(date.getDayOfMonth());
        if (!decisions.isEmpty()) {
            sb.append("\n\n정한 것");
            decisions.forEach(d -> sb.append("\n- ").append(d));
        }
        if (!actions.isEmpty()) {
            sb.append("\n\n누가 언제까지");
            for (Action a : actions) {
                sb.append("\n- ");
                if (notBlank(a.who())) sb.append(a.who()).append(" · ");
                sb.append(a.what() == null ? "" : a.what());
                if (notBlank(a.due())) sb.append(" (").append(a.due()).append(')');
            }
        }
        return sb.toString();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}

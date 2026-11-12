package com.likelion.notebook.meeting.dto;

import com.likelion.notebook.meeting.entity.Meeting.MeetingType;

/** 회의 종류 한글 이름. 공유 원문과 응답에서 같이 씀 */
public final class MeetingLabels {

    private MeetingLabels() {
    }

    public static String typeLabel(MeetingType type) {
        return switch (type) {
            case FIRST -> "첫 회의";
            case IDEA -> "아이디어 회의";
            case CHECK -> "중간 점검 회의";
            case RETRO -> "회고 회의";
        };
    }
}

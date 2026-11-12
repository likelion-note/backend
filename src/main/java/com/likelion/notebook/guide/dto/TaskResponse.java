package com.likelion.notebook.guide.dto;

import com.likelion.notebook.guide.seed.GuideSeed;

/**
 * 할 일 한 줄. action이 있으면 버튼으로 표시
 * - DOC: docKey로 GET /api/guide 의 docs에서 자료를 찾아 시트로 열기
 * - MEETING: meetingType으로 회의 도우미 이동
 */
public record TaskResponse(String key, String title, String day, boolean checked, GuideSeed.Action action) {

    public static TaskResponse of(GuideSeed.Item item, boolean checked) {
        return new TaskResponse(item.key(), item.title(), item.day(), checked, item.action());
    }
}

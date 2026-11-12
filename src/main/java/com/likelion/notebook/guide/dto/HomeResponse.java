package com.likelion.notebook.guide.dto;

import com.likelion.notebook.guide.service.StageService.StageInfo;
import com.likelion.notebook.user.entity.Part;

import java.util.List;

/**
 * SCR-HOME-01 아기사자 홈 (A 담당 부분).
 * B 담당 부분(돌려받음·검토 중 배너, 내 노트 반응, 추천 노트)은 B가 필드를 추가해서 채우기.
 *
 * - assignment: 배정 전이면 null → "운영진이 배정하고 있어요" 카드
 * - stage: 주차·D-day·다음 세션. inProjectPeriod=false면 "지금은 프로젝트 기간이 아니에요"
 * - tasks: 내 파트 × 이번 주차 할 일. 배정 전이거나 기간 밖이면 빈 배열
 */
public record HomeResponse(
        Assignment assignment,
        StageInfo stage,
        String stageLabel,
        List<TaskResponse> tasks,
        TaskSummary taskSummary,
        String opTip
) {

    public record Assignment(Long projectId, String projectTitle, Part part) {
    }

    public record TaskSummary(int done, int total) {
    }
}

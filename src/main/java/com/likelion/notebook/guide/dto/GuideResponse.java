package com.likelion.notebook.guide.dto;

import com.likelion.notebook.guide.seed.GuideSeed;
import com.likelion.notebook.user.entity.Part;

import java.util.List;

/** SCR-GUIDE-01 파트 가이드. 파트 탭·주차 선택에 따라 할 일과 그 할 일이 가리키는 지난 기수 자료 */
public record GuideResponse(
        Part part,
        String stage,
        String stageLabel,
        String currentStage,             // 오늘 기준 주차 구간. 주차 선택의 기본값
        List<GuideSeed.Stage> stages,    // 주차 선택 목록
        List<TaskResponse> tasks,
        List<GuideSeed.Doc> docs,        // tasks의 DOC action이 가리키는 자료만
        String opTip
) {
}

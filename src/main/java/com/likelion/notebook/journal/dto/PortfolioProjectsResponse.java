package com.likelion.notebook.journal.dto;

import java.util.List;

/** 15. 포트폴리오 프로젝트 목록 + 오늘 남은 횟수 */
public record PortfolioProjectsResponse(List<Project> projects, int remainingToday) {

    /** journalCount = 임시 저장 뺀 내 일지 수. 2개 이상이면 available */
    public record Project(Long projectId, String title, long journalCount, boolean available) {
    }
}

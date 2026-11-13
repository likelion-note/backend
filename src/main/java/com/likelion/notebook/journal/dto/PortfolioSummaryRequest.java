package com.likelion.notebook.journal.dto;

import com.likelion.notebook.journal.entity.JournalEnums.PortfolioFormat;

/**
 * 16. 요약 만들기. 빈 값은 PortfolioService에서 400 "입력값을 확인해 주세요".
 * 목록에 없는 format 값은 JSON 변환에서 막힘 → 400 INVALID_INPUT
 */
public record PortfolioSummaryRequest(Long projectId, PortfolioFormat format) {
}

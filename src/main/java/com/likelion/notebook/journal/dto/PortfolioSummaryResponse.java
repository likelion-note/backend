package com.likelion.notebook.journal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.likelion.notebook.journal.entity.JournalEnums.PortfolioFormat;
import com.likelion.notebook.journal.entity.PortfolioSummary;

import java.time.LocalDateTime;
import java.util.List;

/** 16. 만든 요약 + 오늘 남은 횟수 */
public record PortfolioSummaryResponse(
        Long id,
        PortfolioFormat format,
        String result,
        List<Long> sourceJournalIds,
        int remainingToday,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt
) {

    public static PortfolioSummaryResponse of(PortfolioSummary s, int remainingToday) {
        return new PortfolioSummaryResponse(s.getId(), s.getFormat(), s.getResult(),
                List.copyOf(s.getSourceJournalIds()), remainingToday, s.getCreatedAt());
    }
}

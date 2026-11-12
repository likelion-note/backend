package com.likelion.notebook.journal.repository;

import com.likelion.notebook.journal.entity.PortfolioSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface PortfolioSummaryRepository extends JpaRepository<PortfolioSummary, Long> {

    /** 하루 5회 제한: from = 오늘 0시(KST) */
    long countByUserIdAndCreatedAtGreaterThanEqual(Long userId, LocalDateTime from);
}

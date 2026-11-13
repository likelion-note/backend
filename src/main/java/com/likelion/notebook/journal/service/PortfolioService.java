package com.likelion.notebook.journal.service;

import com.likelion.notebook.global.config.ClockConfig;
import com.likelion.notebook.journal.dto.PortfolioProjectsResponse;
import com.likelion.notebook.journal.repository.JournalRepository;
import com.likelion.notebook.journal.repository.PortfolioSummaryRepository;
import com.likelion.notebook.user.entity.ProjectCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 포트폴리오 요약 (15~16번). 근거는 그 프로젝트의 내 일지(임시 저장 제외, 검토 상태 무관) 원문.
 * 미분류 일지는 포트폴리오에서 빠짐. 하루 5회 (KST 기준, 실패는 횟수에 안 들어감)
 */
@Service
@RequiredArgsConstructor
public class PortfolioService {

    static final int DAILY_LIMIT = 5;
    static final int MIN_JOURNALS = 2;

    private final JournalRepository journalRepository;
    private final PortfolioSummaryRepository portfolioSummaryRepository;
    private final Clock clock;

    /** 15. 요약 만들 수 있는 내 프로젝트 + 오늘 남은 횟수 */
    @Transactional(readOnly = true)
    public PortfolioProjectsResponse projects(Long userId) {
        List<PortfolioProjectsResponse.Project> projects = journalRepository.countPortfolioProjects(userId).stream()
                .map(row -> {
                    ProjectCategory p = (ProjectCategory) row[0];
                    long count = (Long) row[1];
                    return new PortfolioProjectsResponse.Project(p.getId(), p.getTitle(), count, count >= MIN_JOURNALS);
                })
                .toList();
        return new PortfolioProjectsResponse(projects, remainingToday(userId));
    }

    int remainingToday(Long userId) {
        long used = portfolioSummaryRepository.countByUserIdAndCreatedAtGreaterThanEqual(userId, startOfToday());
        return (int) Math.max(0, DAILY_LIMIT - used);
    }

    /**
     * 오늘(KST, clock 기준) 0시를 created_at과 같은 시간대(JVM 기본)로 바꾼 값.
     * created_at은 JPA Auditing이 JVM 기본 시간대로 적으므로, 서버가 UTC여도 KST 하루로 셈
     */
    LocalDateTime startOfToday() {
        return LocalDate.now(clock).atStartOfDay(ClockConfig.KST)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}

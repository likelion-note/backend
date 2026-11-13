package com.likelion.notebook.journal.service;

import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.config.ClockConfig;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.journal.ai.AiClient;
import com.likelion.notebook.journal.ai.AiClientException;
import com.likelion.notebook.journal.dto.PortfolioProjectsResponse;
import com.likelion.notebook.journal.dto.PortfolioSummaryRequest;
import com.likelion.notebook.journal.dto.PortfolioSummaryResponse;
import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Status;
import com.likelion.notebook.journal.entity.PortfolioSummary;
import com.likelion.notebook.journal.repository.JournalRepository;
import com.likelion.notebook.journal.repository.PortfolioSummaryRepository;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    static final int DAILY_LIMIT = 5;
    static final int MIN_JOURNALS = 2;

    private final JournalRepository journalRepository;
    private final PortfolioSummaryRepository portfolioSummaryRepository;
    private final UserRepository userRepository;
    private final AiClient aiClient;
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

    /**
     * 16. 요약 만들기. 검사 순서: 입력 → 일지 개수 → 하루 횟수 → AI → 저장.
     * AI가 실패하면 저장 전에 끝나서 행이 안 생김 → 횟수 차감 안 함
     */
    @Transactional
    public PortfolioSummaryResponse create(Long userId, PortfolioSummaryRequest request) {
        if (request == null || request.projectId() == null || request.format() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "입력값을 확인해 주세요");
        }
        List<Journal> journals = journalRepository.findByAuthorIdAndProjectIdAndStatusNotOrderByCreatedAtAsc(
                userId, request.projectId(), Status.DRAFT);
        if (journals.isEmpty()) { // 내 일지가 없는 프로젝트 = 내 프로젝트 아님 (없는 id 포함)
            throw new BusinessException(ErrorCode.INVALID_INPUT, "입력값을 확인해 주세요");
        }
        if (journals.size() < MIN_JOURNALS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "일지가 2개 이상 있어야 해요");
        }
        int remaining = remainingToday(userId);
        if (remaining <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "오늘은 다 만들었어요. 내일 다시 해 주세요");
        }

        ProjectCategory project = journals.get(0).getProject();
        String result;
        try {
            result = aiClient.summarizePortfolio(new AiClient.PortfolioInput(
                    project.getTitle(),
                    journals.get(journals.size() - 1).getPart(),
                    request.format(),
                    journals.stream()
                            .map(j -> new AiClient.PortfolioEntry(j.getActivity(), j.getTiming(), j.getRaw()))
                            .toList()));
        } catch (AiClientException e) {
            log.warn("포트폴리오 요약 실패 userId={} projectId={}", userId, project.getId(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "요약에 실패했어요. 잠시 후 다시 시도해 주세요");
        }

        PortfolioSummary saved = portfolioSummaryRepository.saveAndFlush(PortfolioSummary.builder()
                .user(userRepository.getReferenceById(userId))
                .project(project)
                .format(request.format())
                .result(result)
                .sourceJournalIds(journals.stream().map(Journal::getId).toList())
                .build());
        return PortfolioSummaryResponse.of(saved, remaining - 1);
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

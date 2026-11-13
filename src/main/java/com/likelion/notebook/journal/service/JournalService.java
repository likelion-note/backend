package com.likelion.notebook.journal.service;

import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.guide.repository.EventRepository;
import com.likelion.notebook.guide.service.StageService;
import com.likelion.notebook.journal.ai.AiClient;
import com.likelion.notebook.journal.ai.AiClientException;
import com.likelion.notebook.journal.dto.JournalBanner;
import com.likelion.notebook.journal.dto.JournalDetailResponse;
import com.likelion.notebook.journal.dto.JournalFormResponse;
import com.likelion.notebook.journal.dto.MyJournalsResponse;
import com.likelion.notebook.journal.dto.NoteImpact;
import com.likelion.notebook.journal.entity.JournalEnums.Status;
import com.likelion.notebook.note.repository.NoteReactionRepository;
import com.likelion.notebook.journal.dto.JournalSubmitRequest;
import com.likelion.notebook.journal.dto.JournalSummaryResponse;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.journal.dto.JournalFormResponse.ProjectOption;
import com.likelion.notebook.journal.dto.JournalSaveRequest;
import com.likelion.notebook.journal.dto.JournalSaveResponse;
import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.journal.repository.JournalRepository;
import com.likelion.notebook.user.entity.ProjectAssignment;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.repository.ProjectAssignmentRepository;
import com.likelion.notebook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalService {

    private final UserRepository userRepository;
    private final ProjectAssignmentRepository assignmentRepository;
    private final EventRepository eventRepository;
    private final StageService stageService;
    private final JournalRepository journalRepository;
    private final AiClient aiClient;
    private final NoteReactionRepository reactionRepository;

    private static final int MAX_TAGS = 3;

    /** 1. 작성 화면 기본값 */
    @Transactional(readOnly = true)
    public JournalFormResponse getForm(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        List<ProjectAssignment> assignments = findAllAssignments(userId);
        Optional<ProjectAssignment> current = assignments.stream()
                .filter(ProjectAssignment::isCurrent)
                .findFirst();

        return new JournalFormResponse(
                assignments.stream().map(ProjectOption::of).toList(),
                current.map(a -> a.getProject().getId()).orElse(null),
                current.map(ProjectAssignment::getPart).orElse(user.getPart()),
                defaultTiming(user.getCohort()));
    }

    /** 2. 일지 새로 만들기 (DRAFT). 이후 저장은 update */
    @Transactional
    public JournalSaveResponse create(Long userId, JournalSaveRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        Journal journal = Journal.builder()
                .author(user)
                .project(resolveProject(userId, request.projectId()))
                .part(request.part())
                .activity(request.activity())
                .timing(request.timing())
                .scope(request.scope())
                .raw(request.toRaw())
                .build();
        return JournalSaveResponse.of(journalRepository.saveAndFlush(journal));
    }

    /**
     * 3. 자동 저장. 보낸 body 전체로 덮어씀 (안 보낸 칸은 null). DRAFT·REJECTED만.
     * AI 정리본(summaryTitle, sum*)은 그대로 둠 → 다시 정리(4번)할 때 덮어씀
     */
    @Transactional
    public JournalSaveResponse update(Long userId, Long journalId, JournalSaveRequest request) {
        Journal journal = findMine(userId, journalId, "내 일지만 수정할 수 있어요");
        journal.updateDraft(
                resolveProject(userId, request.projectId()),
                request.part(),
                request.activity(),
                request.timing(),
                request.scope(),
                request.toRaw());
        journalRepository.flush(); // updatedAt을 응답에 담으려고 바로 반영
        return JournalSaveResponse.of(journal);
    }

    /**
     * 4. AI 정리. 저장된 원문으로 제목·태그·이름 가린 정리본을 만들어 저장.
     * 보완 질문은 처음 정리할 때만 받고 DB에 저장하지 않음. AI 실패 시 원문은 그대로 두고 500
     */
    @Transactional
    public JournalSummaryResponse summarize(Long userId, Long journalId) {
        Journal journal = findMine(userId, journalId, "내 일지만 정리할 수 있어요");
        journal.checkReadyForSummary();

        AiClient.JournalSummary result;
        try {
            result = aiClient.summarizeJournal(new AiClient.JournalInput(
                    journal.getPart(), journal.getActivity(), journal.getTiming(), journal.getRaw(),
                    !journal.isSummarized()));
        } catch (AiClientException e) {
            log.warn("AI 정리 실패 journalId={}", journalId, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "정리에 실패했어요. 잠시 후 다시 시도해 주세요");
        }

        List<Tag> tags = result.tags() == null ? List.of()
                : result.tags().stream().filter(Objects::nonNull).distinct().limit(MAX_TAGS).toList();
        journal.applySummary(result.title(), result.body(), tags);

        return new JournalSummaryResponse(
                journal.getSummaryTitle(),
                journal.getTags(),
                JournalSummaryResponse.Body.of(journal.getSummary()),
                AuthorLabels.preview(journal),
                AuthorLabels.audience(journal),
                result.followUpQuestion());
    }

    /** 5. 검토 요청. 태그를 최종 확인하고 PENDING으로 (규칙은 Journal.submit) */
    @Transactional
    public JournalSaveResponse submit(Long userId, Long journalId, JournalSubmitRequest request) {
        Journal journal = findMine(userId, journalId, "내 일지만 요청할 수 있어요");
        journal.submit(request.tags().stream().filter(Objects::nonNull).distinct().toList());
        journalRepository.flush(); // updatedAt(검토 대기열 정렬 기준)을 응답에 담으려고 바로 반영
        return JournalSaveResponse.of(journal);
    }

    /** 6. 내 기록: 통계 + 내 일지 전부 (최근 수정 순, 페이징 없음) */
    @Transactional(readOnly = true)
    public MyJournalsResponse getMine(Long userId) {
        List<Journal> journals = journalRepository.findMineWithProject(userId);
        return new MyJournalsResponse(
                MyJournalsResponse.Stats.of(journals, noteImpact(userId)),
                journals.stream().map(MyJournalsResponse.Item::of).toList());
    }

    /** 7. 내 일지 상세: 원문 + 정리본 (본인만) */
    @Transactional(readOnly = true)
    public JournalDetailResponse getDetail(Long userId, Long journalId) {
        return JournalDetailResponse.of(findMine(userId, journalId, "내 일지만 볼 수 있어요"));
    }

    /** 홈(파트 A)·내 기록: 내 공개 노트의 읽음·도움돼요 합계 */
    @Transactional(readOnly = true)
    public NoteImpact noteImpact(Long userId) {
        return new NoteImpact(
                reactionRepository.countReadsOnAuthorJournals(userId, Status.APPROVED),
                reactionRepository.countHelpsOnAuthorJournals(userId, Status.APPROVED));
    }

    /** 홈(파트 A): 돌려받은·검토 중 내 일지 개수 */
    @Transactional(readOnly = true)
    public JournalBanner journalBanner(Long userId) {
        return new JournalBanner(
                journalRepository.countByAuthorIdAndStatus(userId, Status.REJECTED),
                journalRepository.countByAuthorIdAndStatus(userId, Status.PENDING));
    }

    /** 내 일지 찾기. 없으면 404, 남의 일지면 403 (문구는 API마다 다름) */
    private Journal findMine(Long userId, Long journalId, String forbiddenMessage) {
        Journal journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "일지를 찾을 수 없어요"));
        if (!journal.isWrittenBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, forbiddenMessage);
        }
        return journal;
    }

    /** projectId가 있으면 내가 배정된 적 있는 프로젝트여야 함 (지난 배정 포함). null = 미분류 */
    private ProjectCategory resolveProject(Long userId, Long projectId) {
        if (projectId == null) {
            return null;
        }
        return assignmentRepository.findByUserIdAndProjectId(userId, projectId)
                .map(ProjectAssignment::getProject)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
    }

    /**
     * 내가 배정된 적 있는 배정 전부 (지난 배정·운영 분류 포함). 지금 배정이 맨 앞, 나머지는 최근 배정 순.
     * A의 Repository를 건드리지 않으려고 기존 메서드 두 개를 조합함. 한 사람 배정은 몇 개뿐이라 N번 조회해도 괜찮음.
     */
    private List<ProjectAssignment> findAllAssignments(Long userId) {
        return assignmentRepository.findProjectIdsByUserId(userId).stream()
                .map(projectId -> assignmentRepository.findByUserIdAndProjectId(userId, projectId))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(ProjectAssignment::isCurrent).reversed()
                        .thenComparing(ProjectAssignment::getId, Comparator.reverseOrder()))
                .toList();
    }

    /** 그 기수 일정이 아예 없으면 계산할 수 없어서 null. 있으면 StageService 값 (다 지났으면 AFTER) */
    private Timing defaultTiming(Integer cohort) {
        if (eventRepository.findByCohortOrderByEventDateAsc(cohort).isEmpty()) {
            return null;
        }
        return stageService.current(cohort).timing();
    }
}

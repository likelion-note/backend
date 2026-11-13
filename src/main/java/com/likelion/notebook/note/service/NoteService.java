package com.likelion.notebook.note.service;

import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.note.dto.NoteDetailResponse;
import com.likelion.notebook.note.dto.NoteHelpResponse;
import com.likelion.notebook.note.entity.NoteReaction;
import com.likelion.notebook.journal.repository.JournalRepository;
import com.likelion.notebook.journal.service.AuthorLabels;
import com.likelion.notebook.note.dto.NoteCard;
import com.likelion.notebook.note.dto.NoteFilter;
import com.likelion.notebook.note.dto.NoteFilter.CohortFilter;
import com.likelion.notebook.note.dto.NoteFilter.NoteSort;
import com.likelion.notebook.note.dto.NoteListResponse;
import com.likelion.notebook.note.repository.NoteReactionRepository;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 노트 게시판. 공개 여부는 JournalRepository.findVisibleNotes/findVisibleNote(쿼리)로만 거름.
 * 검색·필터·정렬·페이징은 걸러진 목록에서 서버가 처리 (노트 수가 많지 않음)
 */
@Service
@RequiredArgsConstructor
public class NoteService {

    private final UserRepository userRepository;
    private final JournalRepository journalRepository;
    private final NoteReactionRepository reactionRepository;

    private static final int SIMILAR_LIMIT = 2;

    /** 8. 노트 목록·검색 */
    @Transactional(readOnly = true)
    public NoteListResponse list(Long userId, NoteFilter filter) {
        if (filter.page() < 0 || filter.size() < 1 || filter.size() > NoteFilter.MAX_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        User viewer = findViewer(userId);

        List<Journal> matched = journalRepository.findVisibleNotes(viewer.getId(), viewer.getCohort()).stream()
                .filter(j -> cohortMatches(j, viewer, filter.cohort()))
                .filter(j -> filter.part() == null || filter.part() == j.getPart())
                .filter(j -> filter.timing() == null || filter.timing() == j.getTiming())
                .filter(j -> filter.tag() == null || j.getTags().contains(filter.tag()))
                .filter(j -> NoteSearch.matches(j, filter.q()))
                .toList();

        Map<Long, Counts> counts = counts(ids(matched));
        List<Journal> sorted = matched.stream().sorted(order(filter.sort(), counts)).toList();

        int from = Math.min(filter.page() * filter.size(), sorted.size());
        int to = Math.min(from + filter.size(), sorted.size());
        List<Journal> pageItems = sorted.subList(from, to);

        return new NoteListResponse(
                cards(pageItems, viewer, counts),
                filter.page(), filter.size(), sorted.size(), to < sorted.size());
    }

    /** 9. 노트 상세. 처음 열면 읽음 기록(1인 1회, 작성자 본인 제외). 볼 수 없으면 404 */
    @Transactional
    public NoteDetailResponse detail(Long userId, Long journalId) {
        User viewer = findViewer(userId);
        Journal note = findVisible(journalId, viewer);

        if (!note.isWrittenBy(viewer.getId())
                && !reactionRepository.existsByUserIdAndJournalId(viewer.getId(), note.getId())) {
            reactionRepository.save(new NoteReaction(viewer, note));
        }
        boolean helped = reactionRepository.findByUserIdAndJournalId(viewer.getId(), note.getId())
                .map(NoteReaction::isHelped).orElse(false);

        JournalBody body = note.getSummary();
        return new NoteDetailResponse(
                note.getId(), note.getSummaryTitle(), label(note, viewer),
                isCurrentCohort(note, viewer), note.isWrittenBy(viewer.getId()),
                note.getActivity(), note.getTiming(), note.getTags(),
                new NoteDetailResponse.Body(body.getStuck(), body.getSolved(), body.getAgain(), body.getMemo()),
                reactionRepository.countByJournalId(note.getId()),
                reactionRepository.countByJournalIdAndHelpedTrue(note.getId()),
                helped,
                similarNotes(note, viewer));
    }

    /**
     * 비슷한 노트 최대 2개: 볼 수 있는 노트 중 자기 자신 빼고, 태그 겹침·같은 파트·같은 시점이 있는 것.
     * 점수 = 겹치는 태그 수 × 2 + 파트 같음 1 + 시점 같음 1. 같으면 도움돼요 많은 순 → 최신
     */
    private List<NoteCard> similarNotes(Journal note, User viewer) {
        List<Journal> candidates = journalRepository.findVisibleNotes(viewer.getId(), viewer.getCohort()).stream()
                .filter(j -> !j.getId().equals(note.getId()))
                .filter(j -> similarity(note, j) > 0)
                .toList();
        if (candidates.isEmpty()) {
            return List.of();
        }
        Map<Long, Counts> counts = counts(ids(candidates));
        List<Journal> top = candidates.stream()
                .sorted(Comparator.comparingInt((Journal j) -> similarity(note, j)).reversed()
                        .thenComparing(order(NoteSort.HELPS, counts)))
                .limit(SIMILAR_LIMIT)
                .toList();
        return cards(top, viewer, counts);
    }

    static int similarity(Journal base, Journal other) {
        int sharedTags = (int) other.getTags().stream().filter(base.getTags()::contains).count();
        int score = sharedTags * 2;
        if (base.getPart() != null && base.getPart() == other.getPart()) {
            score++;
        }
        if (base.getTiming() != null && base.getTiming() == other.getTiming()) {
            score++;
        }
        return score;
    }

    /**
     * 10. 도움돼요 토글. note_reaction.helped만 바꿈 (행이 없으면 만들고 true → 읽음도 같이 생김).
     * 볼 수 없으면 404, 내 노트면 403. 알림은 시연 제외
     */
    @Transactional
    public NoteHelpResponse toggleHelp(Long userId, Long journalId) {
        User viewer = findViewer(userId);
        Journal note = findVisible(journalId, viewer);
        if (note.isWrittenBy(viewer.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "내 노트에는 누를 수 없어요");
        }
        NoteReaction reaction = reactionRepository.findByUserIdAndJournalId(viewer.getId(), note.getId())
                .orElseGet(() -> reactionRepository.save(new NoteReaction(viewer, note)));
        boolean helped = reaction.toggleHelp();
        reactionRepository.flush();
        return new NoteHelpResponse(helped, reactionRepository.countByJournalIdAndHelpedTrue(note.getId()));
    }

    /** 볼 수 없거나 없거나 공개 전이면 전부 같은 404 (있다는 것도 알리지 않음) */
    Journal findVisible(Long journalId, User viewer) {
        return journalRepository.findVisibleNote(journalId, viewer.getId(), viewer.getCohort())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "노트를 찾을 수 없어요"));
    }

    User findViewer(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /** 카드 여러 장. isRead는 쿼리 한 번으로 */
    List<NoteCard> cards(List<Journal> notes, User viewer, Map<Long, Counts> counts) {
        if (notes.isEmpty()) {
            return List.of();
        }
        Set<Long> read = new HashSet<>(reactionRepository.findReadJournalIds(viewer.getId(), ids(notes)));
        return notes.stream().map(j -> card(j, viewer, counts.getOrDefault(j.getId(), Counts.ZERO),
                read.contains(j.getId()))).toList();
    }

    NoteCard card(Journal j, User viewer, Counts c, boolean read) {
        NoteSearch.Preview preview = NoteSearch.preview(j);
        return new NoteCard(
                j.getId(), j.getSummaryTitle(), label(j, viewer),
                isCurrentCohort(j, viewer), j.isWrittenBy(viewer.getId()),
                j.getActivity(), j.getTiming(), j.getTags(),
                preview.text(), preview.from(), c.reads(), c.helps(), read);
    }

    /** 같은 기수(내 노트 포함)면 "14기 · PM", 지난 기수면 "지난 기수 B팀 · PM" */
    static String label(Journal j, User viewer) {
        return isCurrentCohort(j, viewer) ? AuthorLabels.sameCohort(j) : AuthorLabels.pastCohort(j);
    }

    static boolean isCurrentCohort(Journal j, User viewer) {
        return !(j.getAuthor().getCohort() < viewer.getCohort());
    }

    /** 노트별 읽음·도움돼요 수 (쿼리 한 번) */
    Map<Long, Counts> counts(Collection<Long> journalIds) {
        Map<Long, Counts> map = new HashMap<>();
        if (journalIds.isEmpty()) {
            return map;
        }
        for (Object[] row : reactionRepository.countByJournalIds(journalIds)) {
            map.put((Long) row[0], new Counts(((Number) row[1]).longValue(),
                    row[2] == null ? 0 : ((Number) row[2]).longValue()));
        }
        return map;
    }

    record Counts(long reads, long helps) {
        static final Counts ZERO = new Counts(0, 0);
    }

    /** NEW: 공개된 순(reviewedAt) 최신. READS·HELPS: 많은 순, 같으면 최신 */
    static Comparator<Journal> order(NoteSort sort, Map<Long, Counts> counts) {
        Comparator<Journal> newest = Comparator
                .comparing((Journal j) -> j.getReviewedAt() == null ? LocalDateTime.MIN : j.getReviewedAt())
                .thenComparing(Journal::getId)
                .reversed();
        if (sort == NoteSort.READS) {
            return Comparator.comparingLong((Journal j) -> counts.getOrDefault(j.getId(), Counts.ZERO).reads())
                    .reversed().thenComparing(newest);
        }
        if (sort == NoteSort.HELPS) {
            return Comparator.comparingLong((Journal j) -> counts.getOrDefault(j.getId(), Counts.ZERO).helps())
                    .reversed().thenComparing(newest);
        }
        return newest;
    }

    private static boolean cohortMatches(Journal j, User viewer, CohortFilter cohort) {
        if (cohort == null || cohort == CohortFilter.ALL) {
            return true;
        }
        return cohort == CohortFilter.NOW ? isCurrentCohort(j, viewer) : !isCurrentCohort(j, viewer);
    }

    static List<Long> ids(List<Journal> journals) {
        return journals.stream().map(Journal::getId).toList();
    }
}

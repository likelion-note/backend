package com.likelion.notebook.note.service;

import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.journal.entity.Journal;
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

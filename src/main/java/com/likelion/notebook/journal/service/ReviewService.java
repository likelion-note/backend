package com.likelion.notebook.journal.service;

import com.likelion.notebook.journal.dto.ReviewListResponse;
import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Status;
import com.likelion.notebook.journal.repository.JournalRepository;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.entity.UserType;
import com.likelion.notebook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 운영진 검토 (11~14번). 운영진 확인은 컨트롤러에서 AdminChecker.require로 먼저 하고 그 User를 받음.
 * 운영진은 자기 기수 일지만 검토 (운영진 기수 = 아기사자 기수).
 * 본인 일지는 검토 대상 아님. 단 그 기수 운영진이 1명뿐이면 허용
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final JournalRepository journalRepository;
    private final UserRepository userRepository;

    /** 11. 검토 목록 */
    @Transactional(readOnly = true)
    public ReviewListResponse list(User admin) {
        boolean includeOwn = isOnlyAdmin(admin);
        List<ReviewListResponse.Pending> pending = journalRepository.findForReview(Status.PENDING, admin.getCohort())
                .stream()
                .filter(j -> includeOwn || !j.isWrittenBy(admin.getId()))
                .map(ReviewListResponse.Pending::of)
                .toList();
        List<ReviewListResponse.Recent> recent = journalRepository
                .findTop10ByAuthorCohortAndStatusInAndReviewedAtIsNotNullOrderByReviewedAtDesc(
                        admin.getCohort(), List.of(Status.APPROVED, Status.REJECTED))
                .stream()
                .map(ReviewListResponse.Recent::of)
                .toList();
        return new ReviewListResponse(pending.size(), pending, recent);
    }

    boolean isOnlyAdmin(User admin) {
        return userRepository.countByCohortAndType(admin.getCohort(), UserType.ADMIN) <= 1;
    }

    static boolean sameCohort(Journal j, User admin) {
        return j.getAuthor().getCohort().equals(admin.getCohort());
    }
}

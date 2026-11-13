package com.likelion.notebook.journal.service;

import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.journal.dto.ReviewDetailResponse;
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

    /** 12. 검토 상세: 공개될 정리본 그대로 (PENDING 외에 처리된 것도 볼 수 있음 — recent에서 열기) */
    @Transactional(readOnly = true)
    public ReviewDetailResponse detail(User admin, Long journalId) {
        return ReviewDetailResponse.of(findReviewable(admin, journalId));
    }

    /** 없음·임시 저장·다른 기수는 전부 404 "일지를 찾을 수 없어요" */
    Journal findReviewable(User admin, Long journalId) {
        return journalRepository.findById(journalId)
                .filter(j -> j.getStatus() != Status.DRAFT)
                .filter(j -> sameCohort(j, admin))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "일지를 찾을 수 없어요"));
    }

    boolean isOnlyAdmin(User admin) {
        return userRepository.countByCohortAndType(admin.getCohort(), UserType.ADMIN) <= 1;
    }

    static boolean sameCohort(Journal j, User admin) {
        return j.getAuthor().getCohort().equals(admin.getCohort());
    }
}

package com.likelion.notebook.journal.repository;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JournalRepository extends JpaRepository<Journal, Long> {

    /** 내 기록 탭 */
    List<Journal> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    /** 운영진 검토 대기열: 오래된 순, 본인 일지 제외 */
    @Query("select j from Journal j where j.status = :status and j.author.id <> :reviewerId order by j.submittedAt asc")
    List<Journal> findByStatusExcludingAuthor(Status status, Long reviewerId);

    default List<Journal> findPendingForReviewer(Long reviewerId) {
        return findByStatusExcludingAuthor(Status.PENDING, reviewerId);
    }

    /** 포트폴리오: 프로젝트별 내 일지 (임시 저장 제외) */
    List<Journal> findByAuthorIdAndProjectIdAndStatusNotOrderByCreatedAtAsc(Long authorId, Long projectId, Status status);

    /*
     * TODO(B): 노트 게시판 조회 쿼리. 공개 조건을 반드시 여기(서버)에서 거르기.
     *   status = APPROVED
     *   and ( cohort < 내 기수
     *         or (cohort = 내 기수 and scope = NOW and project.id not in :내가 거쳐간 프로젝트들) )
     *   + 파트·시점·태그 필터, 검색어(띄어쓰기 무시, 모든 단어 포함)
     * 검색은 같은 기수 노트의 팀 글자로 걸리면 안 됨 (데모 버그였음).
     */
}

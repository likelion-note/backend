package com.likelion.notebook.journal.repository;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JournalRepository extends JpaRepository<Journal, Long> {

    /** 내 기록 탭 */
    List<Journal> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    /** 운영진 검토 대기열: 오래된 순(updatedAt), 본인 일지 제외 */
    @Query("select j from Journal j where j.status = :status and j.author.id <> :reviewerId order by j.updatedAt asc")
    List<Journal> findByStatusExcludingAuthor(Status status, Long reviewerId);

    default List<Journal> findPendingForReviewer(Long reviewerId) {
        return findByStatusExcludingAuthor(Status.PENDING, reviewerId);
    }

    /** 포트폴리오: 프로젝트별 내 일지 (임시 저장 제외) */
    List<Journal> findByAuthorIdAndProjectIdAndStatusNotOrderByCreatedAtAsc(Long authorId, Long projectId, Status status);

    /*
     * TODO(B): 노트 게시판 조회 쿼리. 공개 조건을 반드시 여기(서버)에서 거르기.
     *   status = APPROVED
     *   and ( author.cohort < 내 기수
     *         or (author.cohort = 내 기수 and scope = NOW
     *             and (project is null or 내가 그 project의 배정 행(is_current=false 포함)에 없음)) )
     *   작성자 본인 노트는 항상 보임. 미분류(project null)는 숨기지 않음.
     *   + 파트·시점·태그 필터, 검색어(제목·본문·태그. authorLabel/팀 가명은 검색 안 함)
     * 태그는 JSON 컬럼이라 필터는 문자열 비교(tags like '%"STACK"%')로. H2·MySQL 둘 다 동작.
     */
}

package com.likelion.notebook.journal.repository;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JournalRepository extends JpaRepository<Journal, Long> {

    /**
     * 노트(8~10번) 공개 조건. 익명성 규칙이라 반드시 이 쿼리로만 거르기 (프론트 필터링 금지)
     * status = APPROVED 이고
     *   - 내가 쓴 노트, 또는
     *   - 지난 기수 노트 (scope 상관없음), 또는
     *   - 같은 기수 + NOW + (미분류 또는 내가 그 프로젝트에 배정된 적 없음 — 지난 배정 포함)
     */
    String VISIBLE_NOTE = """
            j.status = com.likelion.notebook.journal.entity.JournalEnums.Status.APPROVED and (
                a.id = :viewerId
                or a.cohort < :cohort
                or (a.cohort = :cohort
                    and j.scope = com.likelion.notebook.journal.entity.JournalEnums.Scope.NOW
                    and (p is null or not exists (
                        select 1 from ProjectAssignment pa where pa.project = p and pa.user.id = :viewerId))))
            """;

    /** 8. 노트 목록: 내가 볼 수 있는 노트 전부 (검색·필터·정렬·페이징은 NoteService) */
    @Query("select j from Journal j join fetch j.author a left join fetch j.project p where " + VISIBLE_NOTE)
    List<Journal> findVisibleNotes(Long viewerId, Integer cohort);

    /** 9·10. 노트 하나. 볼 수 없으면 empty → 404 (있다는 것도 알리지 않음) */
    @Query("select j from Journal j join fetch j.author a left join fetch j.project p where j.id = :journalId and " + VISIBLE_NOTE)
    Optional<Journal> findVisibleNote(Long journalId, Long viewerId, Integer cohort);

    /** 6. 내 기록 탭: 내 일지 전부 + 프로젝트(한 번에), 최근 수정 순 */
    @Query("select j from Journal j left join fetch j.project where j.author.id = :authorId order by j.updatedAt desc, j.id desc")
    List<Journal> findMineWithProject(Long authorId);

    /** 홈 배너: 상태별 내 일지 개수 */
    long countByAuthorIdAndStatus(Long authorId, Status status);

    /** 11. 검토 대기열: 같은 기수의 PENDING, 오래된 순(updatedAt). 본인 일지 제외는 ReviewService (운영진 1명 예외) */
    @Query("select j from Journal j join fetch j.author a where j.status = :status and a.cohort = :cohort "
            + "order by j.updatedAt asc, j.id asc")
    List<Journal> findForReview(Status status, Integer cohort);

    /** 11. 최근 처리한 것: 같은 기수, 승인·돌려보냄, 처리 시각 최신순 10개 (운영진 전체가 처리한 것) */
    List<Journal> findTop10ByAuthorCohortAndStatusInAndReviewedAtIsNotNullOrderByReviewedAtDesc(
            Integer cohort, Collection<Status> statuses);

    /**
     * 15. 포트폴리오 프로젝트 목록: 내 일지(임시 저장 제외)를 프로젝트별로 묶어 [project, 개수, 최근 수정 시각].
     * 미분류(project null)는 inner join이라 빠짐. 최근 수정 순
     */
    @Query("select p, count(j), max(j.updatedAt) from Journal j join j.project p "
            + "where j.author.id = :authorId and j.status <> com.likelion.notebook.journal.entity.JournalEnums.Status.DRAFT "
            + "group by p order by max(j.updatedAt) desc, p.id desc")
    List<Object[]> countPortfolioProjects(Long authorId);

    /** 16. 포트폴리오 근거: 그 프로젝트의 내 일지 (임시 저장 제외), 작성 순 */
    List<Journal> findByAuthorIdAndProjectIdAndStatusNotOrderByCreatedAtAsc(Long authorId, Long projectId, Status status);

}

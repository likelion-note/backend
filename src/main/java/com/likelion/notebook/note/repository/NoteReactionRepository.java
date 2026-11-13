package com.likelion.notebook.note.repository;

import com.likelion.notebook.journal.entity.JournalEnums.Status;
import com.likelion.notebook.note.entity.NoteReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NoteReactionRepository extends JpaRepository<NoteReaction, Long> {

    Optional<NoteReaction> findByUserIdAndJournalId(Long userId, Long journalId);

    boolean existsByUserIdAndJournalId(Long userId, Long journalId);

    /** 이 사람이 쓴 일지(status) 중 읽힌 수 합계. 행 하나 = 읽음 1 (작성자 본인은 행을 만들지 않음) */
    @Query("select count(r) from NoteReaction r where r.journal.author.id = :authorId and r.journal.status = :status")
    long countReadsOnAuthorJournals(Long authorId, Status status);

    /** 노트별 [journalId, 읽음 수, 도움돼요 수]. ids가 비면 부르지 않기 */
    @Query("select r.journal.id, count(r), sum(case when r.helped = true then 1 else 0 end) "
            + "from NoteReaction r where r.journal.id in :journalIds group by r.journal.id")
    List<Object[]> countByJournalIds(Collection<Long> journalIds);

    /** 이 중 내가 읽은 노트 id (isRead) */
    @Query("select r.journal.id from NoteReaction r where r.user.id = :userId and r.journal.id in :journalIds")
    List<Long> findReadJournalIds(Long userId, Collection<Long> journalIds);

    long countByJournalId(Long journalId);

    long countByJournalIdAndHelpedTrue(Long journalId);

    /** 이 사람이 쓴 일지(status) 중 도움돼요 합계 */
    @Query("select count(r) from NoteReaction r where r.journal.author.id = :authorId and r.journal.status = :status and r.helped = true")
    long countHelpsOnAuthorJournals(Long authorId, Status status);
}

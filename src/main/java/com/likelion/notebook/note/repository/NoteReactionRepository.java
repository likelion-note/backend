package com.likelion.notebook.note.repository;

import com.likelion.notebook.journal.entity.JournalEnums.Status;
import com.likelion.notebook.note.entity.NoteReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface NoteReactionRepository extends JpaRepository<NoteReaction, Long> {

    Optional<NoteReaction> findByUserIdAndJournalId(Long userId, Long journalId);

    boolean existsByUserIdAndJournalId(Long userId, Long journalId);

    /** 이 사람이 쓴 일지(status) 중 읽힌 수 합계. 행 하나 = 읽음 1 (작성자 본인은 행을 만들지 않음) */
    @Query("select count(r) from NoteReaction r where r.journal.author.id = :authorId and r.journal.status = :status")
    long countReadsOnAuthorJournals(Long authorId, Status status);

    /** 이 사람이 쓴 일지(status) 중 도움돼요 합계 */
    @Query("select count(r) from NoteReaction r where r.journal.author.id = :authorId and r.journal.status = :status and r.helped = true")
    long countHelpsOnAuthorJournals(Long authorId, Status status);
}

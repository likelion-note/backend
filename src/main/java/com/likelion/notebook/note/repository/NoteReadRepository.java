package com.likelion.notebook.note.repository;

import com.likelion.notebook.note.entity.NoteRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NoteReadRepository extends JpaRepository<NoteRead, Long> {

    boolean existsByUserIdAndJournalId(Long userId, Long journalId);

    /** 추천 노트에서 이미 읽은 것 빼기 */
    @Query("select r.journal.id from NoteRead r where r.user.id = :userId")
    List<Long> findReadJournalIds(Long userId);
}

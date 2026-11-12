package com.likelion.notebook.note.repository;

import com.likelion.notebook.note.entity.NoteReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoteReactionRepository extends JpaRepository<NoteReaction, Long> {

    Optional<NoteReaction> findByUserIdAndJournalId(Long userId, Long journalId);

    boolean existsByUserIdAndJournalId(Long userId, Long journalId);
}

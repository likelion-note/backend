package com.likelion.notebook.note.repository;

import com.likelion.notebook.note.entity.NoteHelp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoteHelpRepository extends JpaRepository<NoteHelp, Long> {

    Optional<NoteHelp> findByUserIdAndJournalId(Long userId, Long journalId);
}

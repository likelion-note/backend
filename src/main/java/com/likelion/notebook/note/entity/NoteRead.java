package com.likelion.notebook.note.entity;

import com.likelion.notebook.global.common.BaseTimeEntity;
import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 읽음 기록. 1인 1회 (unique), 작성자 본인은 기록하지 않음 */
@Getter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "journal_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteRead extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_id")
    private Journal journal;

    public NoteRead(User user, Journal journal) {
        this.user = user;
        this.journal = journal;
    }
}

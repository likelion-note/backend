package com.likelion.notebook.note.entity;

import com.likelion.notebook.global.common.BaseTimeEntity;
import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 읽음·도움돼요를 한 행으로. 행이 있으면 읽음(1인 1회, 작성자 본인은 만들지 않음),
 * helped = true면 도움돼요. 개수는 매번 셈.
 */
@Getter
@Entity
@Table(name = "note_reaction",
        uniqueConstraints = @UniqueConstraint(name = "UK_NOTE_REACTION", columnNames = {"user_id", "journal_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteReaction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_id")
    private Journal journal;

    @Column(nullable = false)
    private boolean helped;

    /** 노트를 처음 열 때 = 읽음 기록 */
    public NoteReaction(User user, Journal journal) {
        this.user = user;
        this.journal = journal;
        this.helped = false;
    }

    /** 도움돼요 토글. 바뀐 값을 돌려줌 */
    public boolean toggleHelp() {
        this.helped = !this.helped;
        return this.helped;
    }
}

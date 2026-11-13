package com.likelion.notebook.note;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Activity;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.journal.repository.JournalRepository;
import com.likelion.notebook.note.entity.NoteReaction;
import com.likelion.notebook.note.repository.NoteReactionRepository;
import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.ProjectCategory.ProjectKind;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.entity.UserType;
import com.likelion.notebook.user.repository.ProjectCategoryRepository;
import com.likelion.notebook.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/** 노트 테스트용 데이터 만들기 */
@Component
public class NoteFixtures {

    static final String STUCK = "중간발표 2주 전에 주제를 바꿔야 할지 고민하다가 일주일을 그냥 보냈어요.";

    @Autowired UserRepository userRepository;
    @Autowired ProjectCategoryRepository projectRepository;
    @Autowired JournalRepository journalRepository;
    @Autowired NoteReactionRepository reactionRepository;

    private User reviewer;

    User member(String sub, int cohort) {
        return userRepository.save(User.builder().email(sub + "@x.com").googleSub(sub)
                .name("민수").type(UserType.MEMBER).cohort(cohort).build());
    }

    ProjectCategory team(String title, int cohort, String letter) {
        return projectRepository.save(ProjectCategory.builder()
                .title(title).kind(ProjectKind.TEAM).cohort(cohort).teamLetter(letter).build());
    }

    /** 승인된 노트 */
    Journal note(User author, ProjectCategory project, Scope scope, Part part, Timing timing,
                 String title, String solved, Tag... tags) {
        Journal j = draftNote(author, project, scope, part, timing, title, solved, tags);
        j.submit(List.of(tags));
        j.approve(reviewer());
        journalRepository.flush();
        return j;
    }

    /** 정리까지만 (승인 전) */
    Journal draftNote(User author, ProjectCategory project, Scope scope, Part part, Timing timing,
                      String title, String solved, Tag... tags) {
        Journal j = journalRepository.save(Journal.builder().author(author).project(project).part(part)
                .activity(Activity.HACKATHON).timing(timing).scope(scope)
                .raw(new JournalBody(STUCK, solved, null, null)).build());
        j.applySummary(title, new JournalBody("정리된 막힌 것: " + title, solved, null, null), List.of(tags));
        return j;
    }

    Journal simpleNote(User author, ProjectCategory project, Scope scope, String title) {
        return note(author, project, scope, Part.PM, Timing.W1_2, title, "넘긴 방법", Tag.DELAY);
    }

    void react(User reader, Journal note, boolean helped) {
        NoteReaction r = new NoteReaction(reader, note);
        if (helped) {
            r.toggleHelp();
        }
        reactionRepository.save(r);
    }

    private User reviewer() {
        if (reviewer == null || userRepository.findById(reviewer.getId()).isEmpty()) {
            reviewer = userRepository.save(User.builder().email("fixture-admin-" + System.nanoTime() + "@x.com")
                    .googleSub("fixture-admin-" + System.nanoTime()).name("운영").type(UserType.ADMIN)
                    .cohort(14).build());
        }
        return reviewer;
    }
}

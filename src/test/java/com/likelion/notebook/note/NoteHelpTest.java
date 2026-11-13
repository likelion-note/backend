package com.likelion.notebook.note;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.note.repository.NoteReactionRepository;
import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.service.AssignmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 10. 도움돼요 (POST /api/notes/{id}/help) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NoteHelpTest {

    @Autowired MockMvc mvc;
    @Autowired NoteFixtures f;
    @Autowired NoteReactionRepository reactionRepository;
    @Autowired AssignmentService assignmentService;

    @Test
    void 누를_때마다_켜지고_꺼진다() throws Exception {
        Journal note = f.simpleNote(f.member("help-author", 13), null, Scope.NEXT, "지난 기수 노트");
        User reader = f.member("help-reader", 14);
        User other = f.member("help-other", 14);

        help(reader, note).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.helped").value(true))
                .andExpect(jsonPath("$.data.helpCount").value(1));
        help(other, note).andExpect(jsonPath("$.data.helpCount").value(2));
        help(reader, note).andExpect(jsonPath("$.data.helped").value(false))
                .andExpect(jsonPath("$.data.helpCount").value(1));

        // 상세를 안 열고 바로 눌러도 읽음이 같이 생김
        assertThat(reactionRepository.countByJournalId(note.getId())).isEqualTo(2);
        mvc.perform(get("/api/notes/" + note.getId()).header("X-User-Id", other.getId()))
                .andExpect(jsonPath("$.data.helped").value(true))
                .andExpect(jsonPath("$.data.readCount").value(2));
    }

    @Test
    void 내_노트는_403_볼_수_없는_노트는_404() throws Exception {
        User me = f.member("help-me", 14);
        User mate = f.member("help-mate", 14);
        ProjectCategory team = f.team("14기 해커톤 · 같은팀", 14, "A");
        assignmentService.assign(me.getId(), team.getId(), Part.PM);
        assignmentService.assign(mate.getId(), team.getId(), Part.BACKEND);

        help(me, f.simpleNote(me, team, Scope.NOW, "내 노트"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("내 노트에는 누를 수 없어요"));

        Journal teammateNote = f.simpleNote(mate, team, Scope.NOW, "팀원 노트");
        help(me, teammateNote)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("노트를 찾을 수 없어요"));
        assertThat(reactionRepository.countByJournalId(teammateNote.getId())).isZero();
    }

    private ResultActions help(User user, Journal note) throws Exception {
        return mvc.perform(post("/api/notes/" + note.getId() + "/help").header("X-User-Id", user.getId()));
    }
}

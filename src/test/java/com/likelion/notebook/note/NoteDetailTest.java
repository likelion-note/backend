package com.likelion.notebook.note;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
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
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 9. 노트 상세 (GET /api/notes/{id}) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NoteDetailTest {

    @Autowired MockMvc mvc;
    @Autowired NoteFixtures f;
    @Autowired NoteReactionRepository reactionRepository;
    @Autowired AssignmentService assignmentService;

    @Test
    void 정리본_전체를_주고_처음_열_때만_읽음을_기록한다() throws Exception {
        User author = f.member("detail-author", 13);
        User reader = f.member("detail-reader", 14);
        ProjectCategory past = f.team("13기 해커톤 · 옛날팀", 13, "B");
        Journal note = f.note(author, past, Scope.NEXT, Part.PM, Timing.W1_2,
                "첫 회의에서 역할이 안 정해졌을 때", "표로 나눴어요", Tag.ROLE_SPLIT);

        open(reader, note)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(note.getId()))
                .andExpect(jsonPath("$.data.title").value("첫 회의에서 역할이 안 정해졌을 때"))
                .andExpect(jsonPath("$.data.authorLabel").value("지난 기수 B팀 · PM"))
                .andExpect(jsonPath("$.data.isCurrentCohort").value(false))
                .andExpect(jsonPath("$.data.isMine").value(false))
                .andExpect(jsonPath("$.data.tags[0]").value("ROLE_SPLIT"))
                .andExpect(jsonPath("$.data.body.solved").value("표로 나눴어요"))
                .andExpect(jsonPath("$.data.readCount").value(1))
                .andExpect(jsonPath("$.data.helped").value(false))
                .andExpect(jsonPath("$.data.author").doesNotExist())
                .andExpect(jsonPath("$.data.projectId").doesNotExist());

        open(reader, note).andExpect(jsonPath("$.data.readCount").value(1)); // 두 번 열어도 1
        assertThat(reactionRepository.countByJournalId(note.getId())).isEqualTo(1);
    }

    @Test
    void 작성자_본인은_읽음을_기록하지_않는다() throws Exception {
        User author = f.member("detail-self", 14);
        Journal note = f.simpleNote(author, null, Scope.NOW, "내 노트");

        open(author, note)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isMine").value(true))
                .andExpect(jsonPath("$.data.authorLabel").value("14기 · PM"))
                .andExpect(jsonPath("$.data.readCount").value(0));
        assertThat(reactionRepository.countByJournalId(note.getId())).isZero();
    }

    @Test
    void 볼_수_없는_노트는_전부_404() throws Exception {
        User viewer = f.member("detail-viewer", 14);
        User mate = f.member("detail-mate", 14);
        ProjectCategory team = f.team("14기 해커톤 · 같은팀", 14, "A");
        assignmentService.assign(viewer.getId(), team.getId(), Part.PM);
        assignmentService.assign(mate.getId(), team.getId(), Part.BACKEND);

        Journal teammateNote = f.simpleNote(mate, team, Scope.NOW, "팀원 노트");
        Journal sameCohortNext = f.simpleNote(f.member("detail-other", 14), null, Scope.NEXT, "다음 기수부터");
        Journal notApproved = f.draftNote(f.member("detail-other2", 14), null, Scope.NOW,
                Part.PM, Timing.W1_2, "승인 전", "방법", Tag.DELAY);

        for (Journal hidden : new Journal[]{teammateNote, sameCohortNext, notApproved}) {
            open(viewer, hidden)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("노트를 찾을 수 없어요"));
        }
        mvc.perform(get("/api/notes/999999").header("X-User-Id", viewer.getId()))
                .andExpect(status().isNotFound());
        assertThat(reactionRepository.countByJournalId(teammateNote.getId())).isZero(); // 숨김 대상은 기록도 안 남음
    }

    @Test
    void 비슷한_노트는_태그_파트_시점_점수순_최대_2개_볼_수_있는_것만() throws Exception {
        User viewer = f.member("sim-viewer", 14);
        User mate = f.member("sim-mate", 14);
        User other = f.member("sim-other", 13);
        ProjectCategory team = f.team("14기 해커톤 · 같은팀", 14, "A");
        assignmentService.assign(viewer.getId(), team.getId(), Part.PM);
        assignmentService.assign(mate.getId(), team.getId(), Part.PM);

        Journal base = f.note(other, null, Scope.NEXT, Part.PM, Timing.W1_2, "기준", "방법",
                Tag.ROLE_SPLIT, Tag.CONFLICT);
        Journal best = f.note(other, null, Scope.NEXT, Part.PM, Timing.W1_2, "태그 둘+파트+시점", "방법",
                Tag.ROLE_SPLIT, Tag.CONFLICT);                                   // 6점
        Journal second = f.note(other, null, Scope.NEXT, Part.DESIGN, Timing.DURING, "태그 하나", "방법",
                Tag.ROLE_SPLIT);                                                 // 2점
        f.note(other, null, Scope.NEXT, Part.DESIGN, Timing.DURING, "시점만", "방법", Tag.STACK); // 0점 → 제외
        f.note(other, null, Scope.NEXT, Part.FRONTEND, Timing.AFTER, "파트만 다름", "방법", Tag.STACK); // 0점
        f.note(mate, team, Scope.NOW, Part.PM, Timing.W1_2, "팀원 노트(숨김)", "방법",
                Tag.ROLE_SPLIT, Tag.CONFLICT);                                   // 점수 높아도 안 보여야 함

        open(viewer, base)
                .andExpect(jsonPath("$.data.similarNotes.length()").value(2))
                .andExpect(jsonPath("$.data.similarNotes[*].id").value(contains(
                        best.getId().intValue(), second.getId().intValue())))
                .andExpect(jsonPath("$.data.similarNotes[0].authorLabel").value("지난 기수 · PM"));
    }

    private ResultActions open(User viewer, Journal note) throws Exception {
        return mvc.perform(get("/api/notes/" + note.getId()).header("X-User-Id", viewer.getId()));
    }
}

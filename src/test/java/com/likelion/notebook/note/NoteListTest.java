package com.likelion.notebook.note;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.journal.entity.JournalEnums.Timing;
import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.service.AssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 8. 노트 목록·검색 (GET /api/notes) — 익명성 규칙 위주.
 * 보는 사람: 14기, 지금 A팀(이전엔 B팀에 배정됐었음)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NoteListTest {

    @Autowired MockMvc mvc;
    @Autowired NoteFixtures f;
    @Autowired AssignmentService assignmentService;

    User viewer;
    Journal teammate, pastTeammate, otherTeam, unclassified, sameCohortNext, pastCohort, mine, nextCohort, pending;

    @BeforeEach
    void setUp() {
        viewer = f.member("list-viewer", 14);
        ProjectCategory teamA = f.team("14기 해커톤 · 멋사수첩팀", 14, "A");
        ProjectCategory teamB = f.team("14기 아이디어톤 · 6조", 14, "B");
        ProjectCategory teamC = f.team("14기 해커톤 · 비밀요원팀", 14, "C");
        ProjectCategory past = f.team("13기 해커톤 · 옛날팀", 13, "B");

        User mateA = f.member("list-mateA", 14);
        User mateB = f.member("list-mateB", 14);
        User otherC = f.member("list-otherC", 14);
        User senior = f.member("list-senior", 13);
        User junior = f.member("list-junior", 15);

        assignmentService.assign(viewer.getId(), teamB.getId(), Part.PM);
        assignmentService.assign(viewer.getId(), teamA.getId(), Part.PM); // 재배정 → B팀은 지난 배정
        assignmentService.assign(mateA.getId(), teamA.getId(), Part.BACKEND);
        assignmentService.assign(mateB.getId(), teamB.getId(), Part.DESIGN);
        assignmentService.assign(otherC.getId(), teamC.getId(), Part.FRONTEND);

        teammate = f.simpleNote(mateA, teamA, Scope.NOW, "지금 팀원 노트");
        pastTeammate = f.simpleNote(mateB, teamB, Scope.NOW, "지난 배정 팀원 노트");
        otherTeam = f.note(otherC, teamC, Scope.NOW, Part.FRONTEND, Timing.DURING,
                "다른 팀 노트", "역할 분담 표를 만들어서 해결했어요", Tag.ROLE_SPLIT, Tag.CONFLICT);
        unclassified = f.note(otherC, null, Scope.NOW, Part.FRONTEND, Timing.W2PLUS,
                "미분류 노트", "회의록을 매번 남겼어요", Tag.MEETING_RUN);
        sameCohortNext = f.simpleNote(otherC, teamC, Scope.NEXT, "같은 기수 다음 기수부터");
        pastCohort = f.note(senior, past, Scope.NEXT, Part.PM, Timing.W1_2,
                "지난 기수 노트", "가".repeat(100), Tag.DELAY);
        mine = f.simpleNote(viewer, teamA, Scope.NOW, "내 노트");
        nextCohort = f.simpleNote(junior, null, Scope.NOW, "다음 기수 노트");
        pending = f.draftNote(otherC, null, Scope.NOW, Part.PM, Timing.W1_2, "승인 전", "방법", Tag.DELAY);
        pending.submit(java.util.List.of(Tag.DELAY));
    }

    @Test
    void 같은_팀원_지난_팀원_같은기수_NEXT_다음기수_미승인은_안_보인다() throws Exception {
        list(get("/api/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(4))
                .andExpect(jsonPath("$.data.notes[*].id").value(containsInAnyOrder(
                        id(otherTeam), id(unclassified), id(pastCohort), id(mine))));
    }

    @Test
    void 팀_가명은_지난_기수_노트에만_붙고_작성자_정보는_없다() throws Exception {
        list(get("/api/notes").param("cohort", "PAST"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.notes[0].authorLabel").value("지난 기수 B팀 · PM"))
                .andExpect(jsonPath("$.data.notes[0].isCurrentCohort").value(false))
                .andExpect(jsonPath("$.data.notes[0].author").doesNotExist())
                .andExpect(jsonPath("$.data.notes[0].projectId").doesNotExist());

        list(get("/api/notes").param("cohort", "NOW"))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.notes[?(@.id == %d)].authorLabel".formatted(otherTeam.getId()))
                        .value("14기 · FRONTEND"))
                .andExpect(jsonPath("$.data.notes[?(@.id == %d)].isMine".formatted(mine.getId())).value(true));
    }

    @Test
    void 같은_기수_노트는_팀_이름으로_검색되지_않는다() throws Exception {
        list(get("/api/notes").param("q", "비밀요원팀")).andExpect(jsonPath("$.data.totalCount").value(0));
        list(get("/api/notes").param("q", "C팀")).andExpect(jsonPath("$.data.totalCount").value(0));
        list(get("/api/notes").param("q", "옛날팀")).andExpect(jsonPath("$.data.totalCount").value(0));
    }

    @Test
    void 검색은_띄어쓰기_무시_여러_단어_모두_포함_태그_이름() throws Exception {
        list(get("/api/notes").param("q", "역할분담"))
                .andExpect(jsonPath("$.data.notes[*].id").value(containsInAnyOrder(id(otherTeam))));
        list(get("/api/notes").param("q", "역할 해결"))
                .andExpect(jsonPath("$.data.totalCount").value(1));
        list(get("/api/notes").param("q", "역할 회의록"))
                .andExpect(jsonPath("$.data.totalCount").value(0));
        list(get("/api/notes").param("q", "의견 충돌")) // 태그 화면 이름도 검색 대상
                .andExpect(jsonPath("$.data.notes[*].id").value(containsInAnyOrder(id(otherTeam))));
        list(get("/api/notes").param("q", "#회의 진행"))
                .andExpect(jsonPath("$.data.notes[*].id").value(containsInAnyOrder(id(unclassified))));
        list(get("/api/notes").param("q", "#없는태그"))
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }

    @Test
    void 필터와_미리보기() throws Exception {
        list(get("/api/notes").param("part", "FRONTEND")).andExpect(jsonPath("$.data.totalCount").value(2));
        list(get("/api/notes").param("timing", "DURING")).andExpect(jsonPath("$.data.totalCount").value(1));
        list(get("/api/notes").param("tag", "MEETING_RUN"))
                .andExpect(jsonPath("$.data.notes[*].id").value(containsInAnyOrder(id(unclassified))));

        list(get("/api/notes").param("cohort", "PAST"))
                .andExpect(jsonPath("$.data.notes[0].previewFrom").value("SOLVED"))
                .andExpect(jsonPath("$.data.notes[0].preview").value(endsWith("…")))
                .andExpect(jsonPath("$.data.notes[0].preview").value("가".repeat(80) + "…"));
    }

    @Test
    void 정렬과_읽음_표시() throws Exception {
        User r1 = f.member("list-r1", 14);
        User r2 = f.member("list-r2", 14);
        f.react(r1, unclassified, true);
        f.react(r2, unclassified, true);
        f.react(r1, pastCohort, false);
        f.react(r2, pastCohort, false);
        f.react(viewer, pastCohort, false);
        f.react(r1, otherTeam, true);

        list(get("/api/notes").param("sort", "HELPS"))
                .andExpect(jsonPath("$.data.notes[0].id").value(id(unclassified)))
                .andExpect(jsonPath("$.data.notes[0].helpCount").value(2))
                .andExpect(jsonPath("$.data.notes[1].id").value(id(otherTeam)));
        list(get("/api/notes").param("sort", "READS"))
                .andExpect(jsonPath("$.data.notes[0].id").value(id(pastCohort)))
                .andExpect(jsonPath("$.data.notes[0].readCount").value(3))
                .andExpect(jsonPath("$.data.notes[0].isRead").value(true));
        list(get("/api/notes")) // NEW: 공개된 순 최신 → 마지막에 승인한 내 노트
                .andExpect(jsonPath("$.data.notes[0].id").value(id(mine)))
                .andExpect(jsonPath("$.data.notes[0].isRead").value(false));
    }

    @Test
    void 페이징과_잘못된_값() throws Exception {
        list(get("/api/notes").param("size", "3").param("page", "0"))
                .andExpect(jsonPath("$.data.notes.length()").value(3))
                .andExpect(jsonPath("$.data.hasNext").value(true));
        list(get("/api/notes").param("size", "3").param("page", "1"))
                .andExpect(jsonPath("$.data.notes.length()").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));

        list(get("/api/notes").param("size", "51")).andExpect(status().isBadRequest());
        list(get("/api/notes").param("page", "-1")).andExpect(status().isBadRequest());
        list(get("/api/notes").param("cohort", "OLD")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mvc.perform(get("/api/notes")).andExpect(status().isUnauthorized());
    }

    private ResultActions list(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("X-User-Id", viewer.getId()));
    }

    private static int id(Journal j) {
        return j.getId().intValue();
    }
}

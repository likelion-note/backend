package com.likelion.notebook.user;

import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.ProjectAssignment;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.ProjectCategory.ProjectKind;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.entity.UserType;
import com.likelion.notebook.user.repository.ProjectAssignmentRepository;
import com.likelion.notebook.user.repository.ProjectCategoryRepository;
import com.likelion.notebook.user.repository.UserRepository;
import com.likelion.notebook.user.service.AssignmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AssignmentServiceTest {

    @Autowired AssignmentService assignmentService;
    @Autowired UserRepository userRepository;
    @Autowired ProjectCategoryRepository projectRepository;
    @Autowired ProjectAssignmentRepository assignmentRepository;

    @Test
    void 파트를_안주면_기본파트를_복사하고_재배정해도_이력이_남는다() {
        User user = userRepository.save(User.builder().email("a@x.com").googleSub("sub-a")
                .name("민수").type(UserType.MEMBER).cohort(14).build());
        ProjectCategory teamA = project("14기 아이디어톤 · 6조", ProjectKind.TEAM);
        ProjectCategory teamB = project("14기 해커톤 · 멋사수첩팀", ProjectKind.TEAM);

        // 배정 전
        assertThat(userRepository.findUnassigned(14)).extracting(User::getId).contains(user.getId());

        // 기본 파트 PM → 파트 없이 배정하면 PM
        assignmentService.changeDefaultPart(user.getId(), Part.PM);
        assignmentService.assign(user.getId(), teamA.getId(), null);
        assertThat(assignmentService.findCurrent(user.getId())).get()
                .extracting(ProjectAssignment::getPart).isEqualTo(Part.PM);

        // 다른 프로젝트에 DESIGN으로 재배정 → 기본 파트는 그대로, 이전 배정은 이력으로
        assignmentService.assign(user.getId(), teamB.getId(), Part.DESIGN);
        ProjectAssignment now = assignmentService.findCurrent(user.getId()).orElseThrow();
        assertThat(now.getProject().getId()).isEqualTo(teamB.getId());
        assertThat(now.getPart()).isEqualTo(Part.DESIGN);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getPart()).isEqualTo(Part.PM);
        assertThat(assignmentRepository.findProjectIdsByUserId(user.getId()))
                .containsExactlyInAnyOrder(teamA.getId(), teamB.getId());
        assertThat(assignmentRepository.findCurrentProjectIdsByUserId(user.getId()))
                .containsExactly(teamB.getId());
        assertThat(userRepository.findUnassigned(14)).extracting(User::getId).doesNotContain(user.getId());

        // 배정 해제 → 다시 배정 전
        assignmentService.unassign(user.getId());
        assertThat(assignmentService.findCurrent(user.getId())).isEmpty();
    }

    @Test
    void 기본파트도_없고_파트도_안주면_배정할_수_없다() {
        User user = userRepository.save(User.builder().email("b@x.com").googleSub("sub-b")
                .name("서연").type(UserType.MEMBER).cohort(14).build());
        ProjectCategory team = project("14기 해커톤 · A", ProjectKind.TEAM);

        assertThatThrownBy(() -> assignmentService.assign(user.getId(), team.getId(), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 운영_분류는_파트가_OPS() {
        User admin = userRepository.save(User.builder().email("c@x.com").googleSub("sub-c")
                .name("운영").type(UserType.ADMIN).cohort(14).build());
        ProjectCategory ops = project("14기 운영", ProjectKind.OPS);

        assignmentService.assign(admin.getId(), ops.getId(), Part.PM);
        assertThat(assignmentService.findCurrent(admin.getId())).get()
                .extracting(ProjectAssignment::getPart).isEqualTo(Part.OPS);
    }

    private ProjectCategory project(String title, ProjectKind kind) {
        return projectRepository.save(ProjectCategory.builder().title(title).kind(kind).cohort(14).build());
    }
}

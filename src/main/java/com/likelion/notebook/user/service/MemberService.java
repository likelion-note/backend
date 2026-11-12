package com.likelion.notebook.user.service;

import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.user.dto.admin.AssignmentResponse;
import com.likelion.notebook.user.dto.admin.MemberView;
import com.likelion.notebook.user.dto.admin.MembersResponse;
import com.likelion.notebook.user.dto.admin.ProjectResponse;
import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.ProjectAssignment;
import com.likelion.notebook.user.entity.ProjectCategory;
import com.likelion.notebook.user.entity.ProjectCategory.ProjectKind;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.repository.ProjectAssignmentRepository;
import com.likelion.notebook.user.repository.ProjectCategoryRepository;
import com.likelion.notebook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** FN-ADM-03 멤버 목록과 배정. 같은 기수 안에서만 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private static final List<Part> TEAM_PARTS = List.of(Part.PM, Part.DESIGN, Part.FRONTEND, Part.BACKEND);

    private final UserRepository userRepository;
    private final ProjectCategoryRepository projectRepository;
    private final ProjectAssignmentRepository assignmentRepository;
    private final AssignmentService assignmentService;

    @Transactional(readOnly = true)
    public MembersResponse members(Integer cohort) {
        List<MemberView> unassigned = userRepository.findUnassigned(cohort).stream().map(MemberView::from).toList();

        List<MembersResponse.TeamGroup> teams = new ArrayList<>();
        List<MembersResponse.OpsGroup> ops = new ArrayList<>();
        for (ProjectCategory p : projectRepository.findByCohortOrderByIdAsc(cohort)) {
            List<ProjectAssignment> current = assignmentRepository.findCurrentMembers(p.getId());
            ProjectResponse project = ProjectResponse.of(p, current.size());
            if (p.getKind() == ProjectKind.OPS) {
                ops.add(new MembersResponse.OpsGroup(project,
                        current.stream().map(a -> MemberView.from(a.getUser())).toList()));
            } else {
                Map<Part, List<MemberView>> byPart = new EnumMap<>(Part.class);
                TEAM_PARTS.forEach(part -> byPart.put(part, new ArrayList<>()));
                current.forEach(a -> byPart.computeIfAbsent(a.getPart(), k -> new ArrayList<>())
                        .add(MemberView.from(a.getUser())));
                teams.add(new MembersResponse.TeamGroup(project, byPart));
            }
        }
        return new MembersResponse(unassigned, teams, ops);
    }

    @Transactional
    public AssignmentResponse assign(Integer adminCohort, Long userId, Long projectId, Part part) {
        User member = sameCohortUser(adminCohort, userId);
        ProjectCategory project = projectRepository.findById(projectId)
                .filter(p -> p.getCohort().equals(adminCohort))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프로젝트 분류를 찾을 수 없어요"));
        if (project.getKind() == ProjectKind.TEAM && part == Part.OPS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "아기사자 팀에는 운영 파트를 줄 수 없어요");
        }
        return AssignmentResponse.from(assignmentService.assign(member.getId(), project.getId(), part));
    }

    @Transactional
    public void unassign(Integer adminCohort, Long userId) {
        assignmentService.unassign(sameCohortUser(adminCohort, userId).getId());
    }

    @Transactional
    public MemberView changeDefaultPart(Integer adminCohort, Long userId, Part part) {
        User member = sameCohortUser(adminCohort, userId);
        if (part == Part.OPS && !member.isAdmin()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "아기사자에게는 운영 파트를 줄 수 없어요");
        }
        assignmentService.changeDefaultPart(member.getId(), part);
        return MemberView.from(member);
    }

    private User sameCohortUser(Integer adminCohort, Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getCohort().equals(adminCohort))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "멤버를 찾을 수 없어요"));
    }
}

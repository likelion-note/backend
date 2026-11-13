package com.likelion.notebook.user.dto.admin;

import com.likelion.notebook.user.entity.Part;

import java.util.List;
import java.util.Map;

/**
 * SCR-ADM-04 멤버·프로젝트
 * - unassigned: 지금 배정이 없는 아기사자. 방금 가입한 사람이 맨 위
 * - teams: 아기사자 팀 분류별 → 파트별 멤버
 * - ops: 운영 분류별 멤버 (파트 구분 없음)
 */
public record MembersResponse(List<MemberView> unassigned, List<TeamGroup> teams, List<OpsGroup> ops) {

    public record TeamGroup(ProjectResponse project, Map<Part, List<MemberView>> membersByPart) {
    }

    public record OpsGroup(ProjectResponse project, List<MemberView> members) {
    }
}

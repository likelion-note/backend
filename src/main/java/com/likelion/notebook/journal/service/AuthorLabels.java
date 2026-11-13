package com.likelion.notebook.journal.service;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalEnums.Scope;
import com.likelion.notebook.user.entity.ProjectCategory;

/**
 * 익명 작성자 표시(authorLabel)와 공개 대상 문구. 이름·id·projectId는 절대 넣지 않음.
 * | 같은 기수 노트          | 14기 · PM               |
 * | 지난 기수 노트          | 지난 기수 B팀 · PM       |
 * | 지난 기수 + 미분류      | 지난 기수 · PM           |
 * | 운영진 글               | 14기 운영진 / 지난 기수 운영진 |
 * | 검토 화면               | 팀 빼고 14기 · PM        |
 * 파트가 비어 있으면 파트를 생략 (예: 14기). 팀 가명은 project_category.anon_label (코드는 아직 teamLetter, 값 "A" → "A팀")
 */
public final class AuthorLabels {

    private static final String SEP = " · ";

    private AuthorLabels() {
    }

    /** 같은 기수가 볼 때, 검토 화면 (팀 없음) */
    public static String sameCohort(Journal journal) {
        int cohort = journal.getAuthor().getCohort();
        if (journal.getAuthor().isAdmin()) {
            return cohort + "기 운영진";
        }
        return withPart(cohort + "기", journal);
    }

    /** 다음 기수 이후가 볼 때 (팀 가명 있음, 미분류면 생략) */
    public static String pastCohort(Journal journal) {
        if (journal.getAuthor().isAdmin()) {
            return "지난 기수 운영진";
        }
        return withPart("지난 기수" + teamSuffix(journal.getProject()), journal);
    }

    /** 4. AI 정리 미리보기: 공개 범위대로 공개되면 보일 표시 */
    public static String preview(Journal journal) {
        return journal.getScope() == Scope.NEXT ? pastCohort(journal) : sameCohort(journal);
    }

    /** 4. AI 정리 미리보기: 누구에게 공개되는지 */
    public static String audience(Journal journal) {
        int cohort = journal.getAuthor().getCohort();
        if (journal.getScope() == Scope.NEXT) {
            return "다음 기수(" + (cohort + 1) + "기)부터";
        }
        return "지금 " + cohort + "기에게 (같은 팀원 제외)";
    }

    private static String withPart(String prefix, Journal journal) {
        return journal.getPart() == null ? prefix : prefix + SEP + journal.getPart().name();
    }

    private static String teamSuffix(ProjectCategory project) {
        if (project == null || project.getTeamLetter() == null || project.getTeamLetter().isBlank()) {
            return "";
        }
        return " " + project.getTeamLetter() + "팀";
    }
}

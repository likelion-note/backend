package com.likelion.notebook.note.service;

import com.likelion.notebook.journal.entity.Journal;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 8. 노트 검색어(q) 처리. 이미 "볼 수 있는 노트"로 걸러진 목록에만 씀.
 * - 띄어쓰기 무시: 검색어와 대상 모두 공백을 빼고 비교 ("역할분담" = "역할 분담")
 * - 여러 단어는 모두 포함
 * - "#"로 시작하면 태그 검색 (태그 화면 이름 또는 값)
 * - 대상: 제목 + 정리본 4칸 + 태그 화면 이름. authorLabel·팀 가명·프로젝트 이름은 절대 넣지 않음
 *   (같은 기수 노트가 팀으로 검색되면 작성자를 짐작할 수 있음)
 */
final class NoteSearch {

    static final int PREVIEW_LENGTH = 80;

    private NoteSearch() {
    }

    static boolean matches(Journal note, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String query = q.strip();
        if (query.startsWith("#")) {
            return findTag(query.substring(1)).map(tag -> note.getTags().contains(tag)).orElse(false);
        }
        String haystack = normalize(searchText(note));
        return Arrays.stream(query.split("\\s+"))
                .map(NoteSearch::normalize)
                .filter(w -> !w.isEmpty())
                .allMatch(haystack::contains);
    }

    /** "#역할 분담", "#역할분담", "#ROLE_SPLIT" → ROLE_SPLIT */
    static Optional<Tag> findTag(String text) {
        String key = normalize(text);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        return Arrays.stream(Tag.values())
                .filter(t -> normalize(t.getLabel()).equals(key) || normalize(t.name()).equals(key))
                .findFirst();
    }

    /** 미리보기: 정리본 "넘긴 방법" 앞부분, 없으면 "막힌 것" */
    static Preview preview(Journal note) {
        JournalBody body = note.getSummary();
        String solved = body == null ? null : body.getSolved();
        if (solved != null && !solved.isBlank()) {
            return new Preview(cut(solved), "SOLVED");
        }
        return new Preview(cut(body == null ? null : body.getStuck()), "STUCK");
    }

    record Preview(String text, String from) {
    }

    private static String searchText(Journal note) {
        JournalBody body = note.getSummary();
        StringBuilder sb = new StringBuilder();
        append(sb, note.getSummaryTitle());
        if (body != null) {
            append(sb, body.getStuck());
            append(sb, body.getSolved());
            append(sb, body.getAgain());
            append(sb, body.getMemo());
        }
        List<Tag> tags = note.getTags();
        tags.forEach(t -> append(sb, t.getLabel()));
        return sb.toString();
    }

    private static void append(StringBuilder sb, String text) {
        if (text != null) {
            sb.append(text).append(' ');
        }
    }

    static String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private static String cut(String text) {
        if (text == null) {
            return null;
        }
        String s = text.strip();
        return s.length() <= PREVIEW_LENGTH ? s : s.substring(0, PREVIEW_LENGTH) + "…";
    }
}

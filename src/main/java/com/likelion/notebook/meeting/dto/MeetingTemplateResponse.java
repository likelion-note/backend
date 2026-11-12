package com.likelion.notebook.meeting.dto;

import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import com.likelion.notebook.meeting.entity.Meeting.MeetingType;
import com.likelion.notebook.meeting.seed.MeetingSeed;

import java.util.List;

/** SCR-MEET-01, 02: 회의 종류 탭, 오늘 정할 질문 3개, 막힐 때 팁 */
public record MeetingTemplateResponse(List<TypeTemplate> types) {

    public record TypeTemplate(MeetingType type, String label, List<MeetingSeed.Question> questions, List<TipView> tips) {
    }

    /** tags는 코드와 한글 라벨을 같이. 관련 노트는 프론트가 노트 게시판 API를 tag로 조회 */
    public record TipView(String title, List<String> steps, String source, List<TagView> tags) {
    }

    public record TagView(Tag code, String label) {
    }

    public static MeetingTemplateResponse of(List<MeetingSeed.Template> templates) {
        return new MeetingTemplateResponse(templates.stream()
                .map(t -> new TypeTemplate(t.type(), MeetingLabels.typeLabel(t.type()), t.questions(),
                        t.tips().stream()
                                .map(tip -> new TipView(tip.title(), tip.steps(), tip.source(),
                                        tip.tags().stream().map(tag -> new TagView(tag, tag.getLabel())).toList()))
                                .toList()))
                .toList());
    }
}

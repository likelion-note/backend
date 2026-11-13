package com.likelion.notebook.journal.ai;

import com.likelion.notebook.journal.ai.AiClient.JournalInput;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Gemini에 보내는 지시문. 응답 모양은 GeminiAiClient의 responseSchema로 고정 */
final class GeminiPrompts {

    static final String MASK = "⟦팀원⟧";

    private GeminiPrompts() {
    }

    static String journal(JournalInput input) {
        JournalBody raw = input.raw();
        String tagList = Arrays.stream(Tag.values())
                .map(t -> "- " + t.name() + ": " + t.getLabel())
                .collect(Collectors.joining("\n"));

        return """
                너는 멋쟁이사자처럼 동아리 앱 "멋사수첩"의 성장일지 정리 도우미야.
                아래 성장일지(프로젝트에서 막혔던 순간과 넘긴 방법)를 다른 기수가 익명으로 읽을 노트로 정리해.

                [규칙]
                1. 사람 이름·닉네임·실명으로 짐작되는 호칭은 전부 "%s"로 바꿔. 팀 이름, 학교·회사 이름, 연락처도 지워.
                2. 내용은 바꾸지 말고, 맞춤법과 문장만 읽기 쉽게 다듬어. 없는 사실을 지어내지 마.
                3. 원문에서 비어 있는 칸(again, memo)은 null로 둬.
                4. title: 어떤 상황이었는지 한 줄 (25자 안팎, 이름 없이). 예: "첫 회의에서 역할이 안 정해졌을 때"
                5. tags: 아래 목록에서 상황에 맞는 것 1~3개. 목록에 없는 값은 쓰지 마.
                %s
                6. followUpQuestion: %s
                7. 말투는 원문 말투(해요체 등)를 따라.

                [일지 정보]
                파트: %s / 활동: %s / 시점: %s

                [막힌 것]
                %s

                [어떻게 넘겼나]
                %s

                [다시 한다면]
                %s

                [메모]
                %s
                """.formatted(
                MASK,
                tagList,
                input.askFollowUp()
                        ? "\"어떻게 넘겼나\"를 더 구체적으로 만들 질문 1개 (짧게, 이름 없이)"
                        : "null",
                label(input.part()), label(input.activity()), label(input.timing()),
                text(raw.getStuck()), text(raw.getSolved()), text(raw.getAgain()), text(raw.getMemo()));
    }

    private static String label(Enum<?> value) {
        return value == null ? "모름" : value.name();
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? "(비어 있음)" : value;
    }
}

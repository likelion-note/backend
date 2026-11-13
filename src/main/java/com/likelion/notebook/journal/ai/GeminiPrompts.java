package com.likelion.notebook.journal.ai;

import com.likelion.notebook.journal.ai.AiClient.JournalInput;
import com.likelion.notebook.journal.ai.AiClient.PortfolioEntry;
import com.likelion.notebook.journal.ai.AiClient.PortfolioInput;
import com.likelion.notebook.journal.entity.JournalEnums.PortfolioFormat;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
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
                   이름에 붙어 있던 "이", "아" 같은 말은 같이 빼고 조사는 자연스럽게 (예: "지영이가" → "⟦팀원⟧이", "민수랑" → "⟦팀원⟧과").
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

    static String portfolio(PortfolioInput input) {
        String format = input.format() == PortfolioFormat.RESUME_3LINES
                ? "이력서에 넣을 3줄. 각 줄은 \"- \"로 시작하는 한 문장이고 줄바꿈으로 구분. 개조식(\"~함\", \"~구현\")으로"
                : "자기소개서·면접용 경험 서술 한 단락 (4~6문장, 줄바꿈 없이). 상황 → 내가 한 일 → 결과 → 배운 점 순서, 합니다체로";
        List<PortfolioEntry> journals = input.journals();
        String entries = IntStream.range(0, journals.size())
                .mapToObj(i -> {
                    PortfolioEntry e = journals.get(i);
                    JournalBody raw = e.raw();
                    return """
                            ## 일지 %d (활동: %s / 시점: %s)
                            [막힌 것] %s
                            [어떻게 넘겼나] %s
                            [다시 한다면] %s
                            [메모] %s
                            """.formatted(i + 1, label(e.activity()), label(e.timing()),
                            text(raw.getStuck()), text(raw.getSolved()), text(raw.getAgain()), text(raw.getMemo()));
                })
                .collect(Collectors.joining("\n"));

        return """
                너는 멋쟁이사자처럼 동아리 앱 "멋사수첩"의 포트폴리오 도우미야.
                아래는 한 사람이 한 프로젝트에서 쓴 성장일지(막혔던 순간과 넘긴 방법)들이야. 이걸 근거로 그 사람의 경험을 요약해.

                [형식]
                %s

                [규칙]
                1. 일지에 적힌 사실만 써. 없는 경험·기술·역할을 지어내지 마.
                2. 숫자·성과·결과물처럼 본인이 직접 채워야 하는 건 지어내지 말고 ⟪숫자⟫, ⟪결과⟫, ⟪기술⟫처럼 ⟪⟫ 빈칸으로 남겨.
                3. 다른 사람 이름은 쓰지 말고 "팀원"으로. 주어는 이 사람(나)으로.
                4. 막혔던 문제를 어떻게 풀었는지가 드러나게 써.
                5. result에 요약 본문만 넣어. 머리말·설명은 붙이지 마.

                [프로젝트] %s
                [파트] %s

                %s
                """.formatted(format, text(input.projectTitle()), label(input.part()), entries);
    }

    private static String label(Enum<?> value) {
        return value == null ? "모름" : value.name();
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? "(비어 있음)" : value;
    }
}

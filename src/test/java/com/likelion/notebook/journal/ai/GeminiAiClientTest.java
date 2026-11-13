package com.likelion.notebook.journal.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.notebook.journal.ai.AiClient.JournalInput;
import com.likelion.notebook.journal.ai.AiClient.JournalSummary;
import com.likelion.notebook.journal.ai.AiClient.PortfolioEntry;
import com.likelion.notebook.journal.ai.AiClient.PortfolioInput;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.PortfolioFormat;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Gemini 실제 호출 없이, 가짜 서버 응답으로 요청 모양과 응답 해석만 확인 */
class GeminiAiClientTest {

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent";

    private MockRestServiceServer server;
    private GeminiAiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiAiClient(builder.build(), new ObjectMapper(), "test-key", "test-model");
    }

    @Test
    void 응답을_정리본으로_바꾸고_목록에_없는_태그는_버린다() {
        String inner = """
                {"title": "첫 회의에서 역할이 안 정해졌을 때",
                 "tags": ["ROLE_SPLIT", "NOPE", "ROLE_SPLIT", "MEETING_RUN", "DELAY", "STACK"],
                 "body": {"stuck": "⟦팀원⟧이 맡을 일이 정해지지 않았어요", "solved": "표로 나눴어요", "again": "지어낸 내용", "memo": null},
                 "followUpQuestion": "표는 누가 만들었나요?"}
                """;
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andRespond(withSuccess(wrap(inner), MediaType.APPLICATION_JSON));

        JournalSummary result = client.summarizeJournal(input(true));

        assertThat(result.title()).isEqualTo("첫 회의에서 역할이 안 정해졌을 때");
        assertThat(result.tags()).containsExactly(Tag.ROLE_SPLIT, Tag.MEETING_RUN, Tag.DELAY);
        assertThat(result.body().getStuck()).isEqualTo("⟦팀원⟧이 맡을 일이 정해지지 않았어요");
        assertThat(result.body().getAgain()).isNull(); // 원문이 비어 있던 칸은 비움
        assertThat(result.followUpQuestion()).isEqualTo("표는 누가 만들었나요?");
        server.verify();
    }

    @Test
    void 보완_질문을_안_물으면_null() {
        server.expect(requestTo(URL)).andRespond(withSuccess(
                wrap("{\"title\": \"제목\", \"tags\": [], \"body\": {\"stuck\": \"a\", \"solved\": \"b\"}, \"followUpQuestion\": \"질문\"}"),
                MediaType.APPLICATION_JSON));

        assertThat(client.summarizeJournal(input(false)).followUpQuestion()).isNull();
    }

    @Test
    void 호출_실패나_이상한_응답은_AiClientException() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(() -> client.summarizeJournal(input(true))).isInstanceOf(AiClientException.class);

        server.reset();
        server.expect(requestTo(URL)).andRespond(withSuccess(wrap("JSON 아님"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.summarizeJournal(input(true))).isInstanceOf(AiClientException.class);

        server.reset();
        server.expect(requestTo(URL)).andRespond(withSuccess("{\"candidates\": []}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.summarizeJournal(input(true))).isInstanceOf(AiClientException.class);
    }

    @Test
    void 포트폴리오_요약은_result만_꺼내고_일지_원문을_보낸다() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(content().string(containsString("표로 나눴어요")))
                .andExpect(content().string(containsString("이력서에 넣을 3줄")))
                .andRespond(withSuccess(wrap("{\"result\": \"  - 역할을 표로 나눠 ⟪숫자⟫일 안에 완성  \"}"),
                        MediaType.APPLICATION_JSON));

        assertThat(client.summarizePortfolio(portfolio())).isEqualTo("- 역할을 표로 나눠 ⟪숫자⟫일 안에 완성");
        server.verify();
    }

    @Test
    void 포트폴리오_요약이_비거나_실패하면_AiClientException() {
        server.expect(requestTo(URL)).andRespond(withSuccess(wrap("{\"result\": \"  \"}"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.summarizePortfolio(portfolio())).isInstanceOf(AiClientException.class);

        server.reset();
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThatThrownBy(() -> client.summarizePortfolio(portfolio())).isInstanceOf(AiClientException.class);
    }

    private static PortfolioInput portfolio() {
        return new PortfolioInput("14기 해커톤 · 멋사수첩팀", null, PortfolioFormat.RESUME_3LINES, List.of(
                new PortfolioEntry(null, null, new JournalBody("역할이 안 정해졌어요", "표로 나눴어요", null, null)),
                new PortfolioEntry(null, null, new JournalBody("배포가 막혔어요", "로그를 봤어요", "미리 연습", null))));
    }

    private static JournalInput input(boolean askFollowUp) {
        return new JournalInput(null, null, null,
                new JournalBody("민수가 맡을 일이 정해지지 않았어요", "표로 나눴어요", null, null), askFollowUp);
    }

    /** Gemini 응답 형식: candidates[0].content.parts[0].text 에 JSON 문자열 */
    private static String wrap(String innerJson) {
        try {
            String text = new ObjectMapper().writeValueAsString(innerJson);
            return "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": " + text + "}]}}]}";
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

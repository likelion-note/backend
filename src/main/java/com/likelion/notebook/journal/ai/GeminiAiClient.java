package com.likelion.notebook.journal.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.notebook.journal.entity.JournalBody;
import com.likelion.notebook.journal.entity.JournalEnums.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Gemini 구현. app.ai.api-key(AI_API_KEY)가 있을 때만 쓰임. 없으면 FakeAiClient.
 * generateContent를 JSON 응답 모드(responseSchema)로 불러서 정해진 모양으로만 받음.
 * 실패(네트워크·키·응답 형식)는 전부 AiClientException. 일지 원문은 로그에 남기지 않음.
 */
@Component
@ConditionalOnExpression("!'${app.ai.api-key:}'.isBlank()")
public class GeminiAiClient implements AiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    @Autowired
    public GeminiAiClient(ObjectMapper objectMapper,
                          @Value("${app.ai.api-key}") String apiKey,
                          @Value("${app.ai.model:gemini-3.5-flash-lite}") String model) {
        this(RestClient.builder().baseUrl(BASE_URL).requestFactory(timeoutFactory()).build(),
                objectMapper, apiKey, model);
    }

    /** 테스트에서 가짜 서버(MockRestServiceServer)에 묶은 RestClient를 넣을 때 */
    GeminiAiClient(RestClient restClient, ObjectMapper objectMapper, String apiKey, String model) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey.trim();
        this.model = model.trim();
    }

    @Override
    public JournalSummary summarizeJournal(JournalInput input) {
        Map<String, Object> request = Map.of(
                "contents", List.of(Map.of("role", "user",
                        "parts", List.of(Map.of("text", GeminiPrompts.journal(input))))),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "responseMimeType", "application/json",
                        "responseSchema", journalSchema()));

        String text = call(request);
        try {
            Output out = objectMapper.readValue(text, Output.class);
            JournalBody raw = input.raw();
            JournalBody body = new JournalBody(
                    out.body() == null ? raw.getStuck() : nvl(out.body().stuck(), raw.getStuck()),
                    out.body() == null ? raw.getSolved() : nvl(out.body().solved(), raw.getSolved()),
                    keepEmpty(raw.getAgain(), out.body() == null ? null : out.body().again()),
                    keepEmpty(raw.getMemo(), out.body() == null ? null : out.body().memo()));
            if (out.title() == null || out.title().isBlank()) {
                throw new AiClientException("Gemini 응답에 제목 없음", null);
            }
            return new JournalSummary(out.title().strip(), body, toTags(out.tags()),
                    input.askFollowUp() ? blankToNull(out.followUpQuestion()) : null);
        } catch (AiClientException e) {
            throw e;
        } catch (Exception e) {
            throw new AiClientException("Gemini 응답 형식 오류", e);
        }
    }

    @Override
    public String summarizePortfolio(PortfolioInput input) {
        Map<String, Object> schema = Map.of("type", "OBJECT",
                "properties", Map.of("result", Map.of("type", "STRING")),
                "required", List.of("result"));
        Map<String, Object> request = Map.of(
                "contents", List.of(Map.of("role", "user",
                        "parts", List.of(Map.of("text", GeminiPrompts.portfolio(input))))),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "responseMimeType", "application/json",
                        "responseSchema", schema));

        String text = call(request);
        String result;
        try {
            result = objectMapper.readValue(text, PortfolioOutput.class).result();
        } catch (Exception e) {
            throw new AiClientException("Gemini 응답 형식 오류", e);
        }
        if (result == null || result.isBlank()) {
            throw new AiClientException("Gemini 응답에 요약 없음", null);
        }
        return result.strip();
    }

    /** generateContent 호출 → 첫 후보의 텍스트(JSON 문자열) */
    private String call(Map<String, Object> request) {
        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new AiClientException("Gemini 호출 실패", e);
        }
        JsonNode text = response == null ? null
                : response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (text == null || !text.isTextual() || text.asText().isBlank()) {
            throw new AiClientException("Gemini 응답이 비어 있음", null);
        }
        return text.asText();
    }

    private static Map<String, Object> journalSchema() {
        Map<String, Object> str = Map.of("type", "STRING");
        Map<String, Object> nullableStr = Map.of("type", "STRING", "nullable", true);

        Map<String, Object> bodyProps = new LinkedHashMap<>();
        bodyProps.put("stuck", str);
        bodyProps.put("solved", str);
        bodyProps.put("again", nullableStr);
        bodyProps.put("memo", nullableStr);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("title", str);
        props.put("tags", Map.of("type", "ARRAY",
                "items", Map.of("type", "STRING", "enum", Arrays.stream(Tag.values()).map(Tag::name).toList())));
        props.put("body", Map.of("type", "OBJECT", "properties", bodyProps, "required", List.of("stuck", "solved")));
        props.put("followUpQuestion", nullableStr);

        return Map.of("type", "OBJECT", "properties", props, "required", List.of("title", "tags", "body"));
    }

    /** 12개 목록에 없는 값은 버리고, 중복 빼고 최대 3개 */
    private static List<Tag> toTags(List<String> names) {
        if (names == null) {
            return List.of();
        }
        return names.stream().filter(Objects::nonNull)
                .map(n -> Arrays.stream(Tag.values()).filter(t -> t.name().equals(n.trim())).findFirst().orElse(null))
                .filter(Objects::nonNull).distinct().limit(3).toList();
    }

    /** 원문이 비어 있던 칸은 정리본도 비움 (AI가 지어내지 않게) */
    private static String keepEmpty(String rawValue, String summarized) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return nvl(summarized, rawValue);
    }

    private static String nvl(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static JdkClientHttpRequestFactory timeoutFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Output(String title, List<String> tags, OutputBody body, String followUpQuestion) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PortfolioOutput(String result) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OutputBody(String stuck, String solved, String again, String memo) {
    }
}

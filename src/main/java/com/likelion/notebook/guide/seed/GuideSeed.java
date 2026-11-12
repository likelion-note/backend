package com.likelion.notebook.guide.seed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.notebook.user.entity.Part;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * seed/guide.json 을 앱 시작 시 한 번 읽어 메모리에 둠 (DB에 안 넣음).
 * - stages: 주차 구간(W1, W2, W3, W4_5, W6, W7) + 운영진 한마디
 * - items : 파트 × 주차 할 일. key(예: PM-W2-0)가 GuideCheck.itemKey
 * - docs  : 지난 기수 자료
 */
@Component
@RequiredArgsConstructor
public class GuideSeed {

    private final ObjectMapper objectMapper;

    private List<Stage> stages;
    private List<Item> items;
    private Map<String, Item> itemByKey;
    private Map<String, Doc> docByKey;

    @PostConstruct
    void load() throws IOException {
        try (InputStream in = new ClassPathResource("seed/guide.json").getInputStream()) {
            SeedFile file = objectMapper.readValue(in, SeedFile.class);
            this.stages = List.copyOf(file.stages());
            this.items = file.items().stream()
                    .sorted(Comparator.comparing(Item::order))
                    .toList();
            this.itemByKey = items.stream().collect(Collectors.toMap(Item::key, Function.identity()));
            this.docByKey = file.docs().stream().collect(Collectors.toMap(Doc::key, Function.identity()));
        }
    }

    public List<Stage> stages() {
        return stages;
    }

    public Optional<Stage> stage(String stage) {
        return stages.stream().filter(s -> s.stage().equals(stage)).findFirst();
    }

    public List<Item> items(Part part, String stage) {
        return items.stream()
                .filter(i -> i.part() == part && i.stage().equals(stage))
                .toList();
    }

    public Optional<Item> item(String key) {
        return Optional.ofNullable(itemByKey.get(key));
    }

    public Optional<Doc> doc(String key) {
        return Optional.ofNullable(key == null ? null : docByKey.get(key));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeedFile(List<Stage> stages, List<Item> items, List<Doc> docs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stage(String stage, String label, String opTip) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String key, Part part, String stage, Integer order, String title, String day, Action action) {
    }

    /** type = DOC(지난 기수 자료 열기) | MEETING(회의 도우미로 이동) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Action(String type, String docKey, String meetingType, String label) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Doc(String key, String title, String source, List<String> lines) {
    }
}

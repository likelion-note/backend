package com.likelion.notebook.guide.service;

import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import com.likelion.notebook.guide.dto.CheckRequest;
import com.likelion.notebook.guide.dto.GuideResponse;
import com.likelion.notebook.guide.dto.HomeResponse;
import com.likelion.notebook.guide.dto.TaskResponse;
import com.likelion.notebook.guide.entity.GuideCheck;
import com.likelion.notebook.guide.repository.GuideCheckRepository;
import com.likelion.notebook.guide.seed.GuideSeed;
import com.likelion.notebook.guide.service.StageService.StageInfo;
import com.likelion.notebook.user.entity.Part;
import com.likelion.notebook.user.entity.ProjectAssignment;
import com.likelion.notebook.user.entity.User;
import com.likelion.notebook.user.repository.UserRepository;
import com.likelion.notebook.user.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GuideService {

    private final GuideSeed guideSeed;
    private final StageService stageService;
    private final AssignmentService assignmentService;
    private final UserRepository userRepository;
    private final GuideCheckRepository guideCheckRepository;

    /** GET /api/home. 운영진은 첫 탭이 검토라 홈 없음 → FORBIDDEN */
    @Transactional(readOnly = true)
    public HomeResponse home(Long userId) {
        User user = findUser(userId);
        if (user.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "운영진은 홈 대신 검토 탭을 써요");
        }
        StageInfo stage = stageService.current(user.getCohort());
        ProjectAssignment current = assignmentService.findCurrent(userId).orElse(null);

        HomeResponse.Assignment assignment = current == null ? null
                : new HomeResponse.Assignment(current.getProject().getId(), current.getProject().getTitle(), current.getPart());

        List<TaskResponse> tasks = (current == null || !stage.inProjectPeriod() || stage.stage() == null)
                ? List.of()
                : tasks(userId, current.getPart(), stage.stage());

        int done = (int) tasks.stream().filter(TaskResponse::checked).count();

        return new HomeResponse(assignment, stage, stageLabel(stage.stage()), tasks,
                new HomeResponse.TaskSummary(done, tasks.size()), opTip(stage.stage()));
    }

    /** GET /api/guide. part·stage를 안 주면 내 파트·이번 주차 */
    @Transactional(readOnly = true)
    public GuideResponse guide(Long userId, Part part, String stage) {
        User user = findUser(userId);
        String currentStage = stageService.current(user.getCohort()).stage();

        Part resolvedPart = part != null ? part
                : assignmentService.findCurrent(userId).map(ProjectAssignment::getPart).orElse(user.getPart());
        if (resolvedPart == null || resolvedPart == Part.OPS) {
            resolvedPart = Part.PM;
        }
        String resolvedStage = stage != null ? stage : (currentStage != null ? currentStage : "W1");
        if (guideSeed.stage(resolvedStage).isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "없는 주차예요");
        }

        List<TaskResponse> tasks = tasks(userId, resolvedPart, resolvedStage);
        List<GuideSeed.Doc> docs = tasks.stream()
                .map(TaskResponse::action)
                .filter(Objects::nonNull)
                .map(GuideSeed.Action::docKey)
                .distinct()
                .map(guideSeed::doc)
                .flatMap(java.util.Optional::stream)
                .toList();

        return new GuideResponse(resolvedPart, resolvedStage, stageLabel(resolvedStage), currentStage,
                guideSeed.stages(), tasks, docs, opTip(resolvedStage));
    }

    /** PUT /api/guide/checks/{itemKey}. true면 체크, false면 해제. 여러 번 보내도 결과 같음 */
    @Transactional
    public TaskResponse check(Long userId, String itemKey, CheckRequest request) {
        GuideSeed.Item item = guideSeed.item(itemKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "없는 할 일이에요"));
        var existing = guideCheckRepository.findByUserIdAndItemKey(userId, itemKey);

        if (request.checked() && existing.isEmpty()) {
            guideCheckRepository.save(new GuideCheck(findUser(userId), itemKey));
        } else if (!request.checked()) {
            existing.ifPresent(guideCheckRepository::delete);
        }
        return TaskResponse.of(item, request.checked());
    }

    private List<TaskResponse> tasks(Long userId, Part part, String stage) {
        Set<String> checked = new HashSet<>(guideCheckRepository.findCheckedKeys(userId));
        return guideSeed.items(part, stage).stream()
                .map(i -> TaskResponse.of(i, checked.contains(i.key())))
                .toList();
    }

    private String stageLabel(String stage) {
        return stage == null ? null : guideSeed.stage(stage).map(GuideSeed.Stage::label).orElse(null);
    }

    private String opTip(String stage) {
        return stage == null ? null : guideSeed.stage(stage).map(GuideSeed.Stage::opTip).orElse(null);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}

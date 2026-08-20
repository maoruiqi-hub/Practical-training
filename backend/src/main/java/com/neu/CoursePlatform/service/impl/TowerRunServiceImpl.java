package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.agentic.AgenticRequest;
import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.common.SharedIds;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.common.event.GameEventPublisher;
import com.neu.CoursePlatform.common.event.TowerDiagnosisRequestedEvent;
import com.neu.CoursePlatform.entity.AbilityKnowledgePoint;
import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.entity.KnowledgeMastery;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.StudentAbilityDeltaLog;
import com.neu.CoursePlatform.entity.StudentTowerAttempt;
import com.neu.CoursePlatform.entity.StudentTowerNode;
import com.neu.CoursePlatform.entity.StudentTowerQuestionPack;
import com.neu.CoursePlatform.entity.StudentTowerRun;
import com.neu.CoursePlatform.mapper.AbilityKnowledgePointMapper;
import com.neu.CoursePlatform.mapper.KnowledgeMasteryMapper;
import com.neu.CoursePlatform.mapper.StudentAbilityDeltaLogMapper;
import com.neu.CoursePlatform.mapper.StudentTowerAttemptMapper;
import com.neu.CoursePlatform.mapper.StudentTowerNodeMapper;
import com.neu.CoursePlatform.mapper.StudentTowerQuestionPackMapper;
import com.neu.CoursePlatform.mapper.StudentTowerRunMapper;
import com.neu.CoursePlatform.service.AbilityRadarService;
import com.neu.CoursePlatform.service.AbilitySnapshotService;
import com.neu.CoursePlatform.service.AbilityPointService;
import com.neu.CoursePlatform.service.FloorProgressService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.LearningEvidenceService;
import com.neu.CoursePlatform.service.TowerRunService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class TowerRunServiceImpl implements TowerRunService {
    private static final Set<String> BATTLE_TYPES = Set.of("battle", "elite", "boss");

    private final StudentTowerRunMapper runMapper;
    private final StudentTowerNodeMapper nodeMapper;
    private final StudentTowerAttemptMapper attemptMapper;
    private final StudentAbilityDeltaLogMapper deltaMapper;
    private final KnowledgePointService knowledgePointService;
    private final AbilityPointService abilityPointService;
    private final AbilityKnowledgePointMapper abilityKnowledgePointMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final StudentTowerQuestionPackMapper questionPackMapper;
    private final LearningEvidenceService learningEvidenceService;
    private final AbilitySnapshotService abilitySnapshotService;
    private final AbilityRadarService abilityRadarService;
    private final FloorProgressService floorProgressService;
    private final GameEventPublisher eventPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AgenticClient agenticClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TowerRunServiceImpl(StudentTowerRunMapper runMapper,
                               StudentTowerNodeMapper nodeMapper,
                               StudentTowerAttemptMapper attemptMapper,
                               StudentAbilityDeltaLogMapper deltaMapper,
                               KnowledgePointService knowledgePointService,
                               AbilityPointService abilityPointService,
                               AbilityKnowledgePointMapper abilityKnowledgePointMapper,
                               KnowledgeMasteryMapper knowledgeMasteryMapper,
                               StudentTowerQuestionPackMapper questionPackMapper,
                               LearningEvidenceService learningEvidenceService,
                               AbilitySnapshotService abilitySnapshotService,
                               AbilityRadarService abilityRadarService,
                               FloorProgressService floorProgressService,
                               GameEventPublisher eventPublisher,
                               ApplicationEventPublisher applicationEventPublisher,
                               AgenticClient agenticClient) {
        this.runMapper = runMapper;
        this.nodeMapper = nodeMapper;
        this.attemptMapper = attemptMapper;
        this.deltaMapper = deltaMapper;
        this.knowledgePointService = knowledgePointService;
        this.abilityPointService = abilityPointService;
        this.abilityKnowledgePointMapper = abilityKnowledgePointMapper;
        this.knowledgeMasteryMapper = knowledgeMasteryMapper;
        this.questionPackMapper = questionPackMapper;
        this.learningEvidenceService = learningEvidenceService;
        this.abilitySnapshotService = abilitySnapshotService;
        this.abilityRadarService = abilityRadarService;
        this.floorProgressService = floorProgressService;
        this.eventPublisher = eventPublisher;
        this.applicationEventPublisher = applicationEventPublisher;
        this.agenticClient = agenticClient;
    }

    @Override
    public Map<String, Object> getOrCreateActiveRun(String studentNo, String courseCode) {
        StudentTowerRun run = activeRun(studentNo, courseCode);
        if (run == null) return generateRun(studentNo, courseCode, false);
        return toRunDto(run, nodes(run.getRunId()));
    }

    @Override
    @Transactional
    public Map<String, Object> generateRun(String studentNo, String courseCode, boolean force) {
        StudentTowerRun current = activeRun(studentNo, courseCode);
        if (current != null && !force) return toRunDto(current, nodes(current.getRunId()));
        if (current != null) {
            current.setStatus("archived");
            current.setUpdatedAt(LocalDateTime.now());
            runMapper.updateById(current);
        }

        StudentTowerRun run = new StudentTowerRun();
        run.setRunId(SharedIds.newId());
        run.setStudentNo(studentNo);
        run.setCourseCode(courseCode);
        run.setVersion(nextVersion(studentNo, courseCode));
        run.setStatus("active");
        run.setRouteSource("rule");
        run.setCreatedAt(LocalDateTime.now());
        run.setUpdatedAt(LocalDateTime.now());

        List<StudentTowerNode> planned = planNodes(run, studentNo, courseCode);
        if (planned.isEmpty()) {
            planned = fallbackNodes(run, knowledgePoints(courseCode), Map.of(), Map.of());
        }
        if (planned.isEmpty()) {
            throw new IllegalStateException("课程尚未配置知识点，无法生成学习路线");
        }
        planned.sort(Comparator.comparing(StudentTowerNode::getNodeOrder));
        String firstAvailable = planned.stream()
                .filter(node -> "available".equals(node.getStatus()))
                .map(StudentTowerNode::getNodeId)
                .findFirst()
                .orElse(planned.get(0).getNodeId());
        run.setCurrentNodeId(firstAvailable);
        try {
            runMapper.insert(run);
        } catch (DuplicateKeyException e) {
            if (!force) {
                StudentTowerRun existing = activeRun(studentNo, courseCode);
                if (existing != null) return toRunDto(existing, nodes(existing.getRunId()));
            }
            throw e;
        }
        planned.forEach(nodeMapper::insert);
        return toRunDto(run, planned);
    }

    @Override
    public Map<String, Object> getNode(String studentNo, String runId, String nodeId) {
        StudentTowerRun run = requireRun(studentNo, runId);
        StudentTowerNode node = requireNode(runId, nodeId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("run", toRunDto(run, nodes(runId)));
        result.put("node", toNodeDto(node, supportIndexes(run.getCourseCode())));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> enterNode(String studentNo, String runId, String nodeId) {
        StudentTowerRun run = requireRun(studentNo, runId);
        StudentTowerNode node = requireNode(runId, nodeId);
        if ("locked".equals(node.getStatus()) || "disabled".equals(node.getStatus())) {
            throw new IllegalStateException("节点尚未解锁");
        }
        run.setCurrentNodeId(nodeId);
        run.setUpdatedAt(LocalDateTime.now());
        runMapper.updateById(run);
        String evaluationId = SharedIds.newId();
        abilitySnapshotService.createBeforeSnapshots(evaluationId, studentNo, run.getCourseCode(), runId, nodeId);
        Map<String, Object> result = new LinkedHashMap<>(getNode(studentNo, runId, nodeId));
        result.put("evaluationId", evaluationId);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> completeNode(String studentNo, String runId, String nodeId, Map<String, Object> request) {
        StudentTowerRun run = requireRun(studentNo, runId);
        StudentTowerNode node = requireNode(runId, nodeId);
        if ("locked".equals(node.getStatus())) throw new IllegalStateException("节点尚未解锁");

        String evaluationId = evaluationId(request, studentNo, run, node);
        StudentTowerAttempt previousAttempt = attemptMapper.selectById(evaluationId);
        if (previousAttempt != null) return completedResponse(run, node, previousAttempt);

        LearningEvidenceService.BatchResult evidence = learningEvidenceService.recordVerifiedAnswers(
                studentNo, run.getCourseCode(), evaluationId, reportStageFor(node), answerList(request),
                allowedQuestionIds(request, studentNo, run, node));

        String result = stringValue(request, "result");
        if (result == null || result.isBlank()) result = Boolean.TRUE.equals(request.get("cleared")) ? "cleared" : "failed";
        String reportStage = reportStageFor(node);
        RateStats rateStats = new RateStats(evidence.correctRate(), evidence.correctCount(), evidence.gradedCount(), "server_evidence");
        double correctRate = rateStats.correctRate;
        boolean cleared = isClearedResult(result) && correctRate >= ("boss".equals(node.getRoomType()) ? 0.75D : 0.70D);
        result = cleared ? "cleared" : "failed";
        Map<String, Object> reportEnvelope = reportEnvelope("pending", null, "async", null, true, false);
        Map<String, Object> verifiedRequest = new LinkedHashMap<>(request);
        verifiedRequest.put("answerSummary", verifiedAnswerSummary(answerList(request), evidence.answers()));
        recordAttempt(evaluationId, run, node, studentNo, result, correctRate, verifiedRequest, reportEnvelope);
        applyCompletion(run, node, result, cleared, correctRate, intValue(request.get("hpLeft"), intValue(request.get("hp_left"), 0)));
        abilitySnapshotService.createAfterSnapshots(evaluationId);
        applicationEventPublisher.publishEvent(new TowerDiagnosisRequestedEvent(evaluationId));
        Map<String, Object> response = new LinkedHashMap<>(toRunDto(runMapper.selectById(runId), nodes(runId)));
        response.put("evaluationId", evaluationId);
        response.put("correctRate", correctRate);
        response.put("battleCorrectRate", correctRate);
        response.put("cleared", cleared);
        response.put("correctRateSource", rateStats.source);
        response.put("gradedCount", rateStats.gradedCount);
        response.put("correctCount", rateStats.correctCount);
        response.put("answerResults", evidence.answers());
        response.put("abilityRadar", abilityRadarService.getAbilityRadar(studentNo, run.getCourseCode(), runId, nodeId));
        response.putAll(reportEnvelope);
        return response;
    }

    @Override
    @Transactional
    public Map<String, Object> diagnoseNode(String studentNo, String runId, String nodeId, Map<String, Object> request) {
        StudentTowerRun run = requireRun(studentNo, runId);
        StudentTowerNode node = requireNode(runId, nodeId);
        String evaluationId = evaluationId(request, studentNo, run, node);
        StudentTowerAttempt previousAttempt = attemptMapper.selectById(evaluationId);
        if (previousAttempt != null) return completedDiagnosisResponse(run, node, previousAttempt);
        LearningEvidenceService.BatchResult evidence = learningEvidenceService.recordVerifiedAnswers(
                studentNo, run.getCourseCode(), evaluationId, "diagnosis_room", answerList(request),
                allowedQuestionIds(request, studentNo, run, node));
        RateStats rateStats = new RateStats(evidence.correctRate(), evidence.correctCount(), evidence.gradedCount(), "server_evidence");
        double correctRate = rateStats.gradedCount > 0 ? rateStats.correctRate : 0D;
        boolean perfect = correctRate >= 0.999D;
        StudentTowerNode elite = boundEliteNode(runId, node);
        recordAttempt(evaluationId, run, node, studentNo, perfect ? "diagnosis_perfect" : "diagnosis_partial", correctRate, request, null);
        applyCompletion(run, node, perfect ? "diagnosis_perfect" : "diagnosis_partial", true, correctRate, 0);
        abilitySnapshotService.createAfterSnapshots(evaluationId);
        if (perfect && elite != null && !"cleared".equals(elite.getStatus())) {
            recordAttempt(SharedIds.newId(), run, elite, studentNo, "diagnosis_perfect", correctRate, request, null);
            applyCompletion(run, elite, "diagnosis_perfect", true, correctRate, 0);
        } else if (!perfect && elite != null && "locked".equals(elite.getStatus())) {
            elite.setStatus("available");
            elite.setUpdatedAt(LocalDateTime.now());
            nodeMapper.updateById(elite);
            updateCurrentNode(run);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", perfect ? "perfect" : correctRate >= 0.5D ? "partial" : "weak");
        result.put("evaluationId", evaluationId);
        result.put("correctRate", correctRate);
        result.put("correctRateSource", rateStats.source);
        result.put("gradedCount", rateStats.gradedCount);
        result.put("correctCount", rateStats.correctCount);
        result.put("answerResults", evidence.answers());
        result.put("diagnosisPassed", perfect);
        result.put("eliteBypassed", perfect && elite != null);
        result.put("battleBypassed", perfect && elite != null);
        result.put("clearedEliteNodeId", perfect && elite != null ? elite.getNodeId() : null);
        result.put("nextNodeId", !perfect && elite != null ? elite.getNodeId() : null);
        result.put("nextRoomType", !perfect && elite != null ? elite.getRoomType() : null);
        result.put("nextNode", !perfect && elite != null ? toNodeDto(elite, supportIndexes(run.getCourseCode())) : null);
        result.putAll(reportEnvelope("skipped", null, "diagnosis_gate", null, true, false));
        result.put("abilityRadar", abilityRadarService.getAbilityRadar(studentNo, run.getCourseCode(), runId, nodeId));
        result.put("run", toRunDto(runMapper.selectById(runId), nodes(runId)));
        return result;
    }

    @Override
    public List<Map<String, Object>> getAbilityDeltas(String studentNo, String courseCode, String runId) {
        LambdaQueryWrapper<StudentAbilityDeltaLog> q = new LambdaQueryWrapper<StudentAbilityDeltaLog>()
                .eq(StudentAbilityDeltaLog::getStudentNo, studentNo)
                .eq(StudentAbilityDeltaLog::getCourseCode, courseCode)
                .orderByDesc(StudentAbilityDeltaLog::getCreatedAt);
        if (runId != null && !runId.isBlank()) q.eq(StudentAbilityDeltaLog::getRunId, runId);
        return deltaMapper.selectList(q).stream().limit(30).map(delta -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", delta.getId());
            item.put("runId", delta.getRunId());
            item.put("nodeId", delta.getNodeId());
            item.put("knowledgePointId", delta.getKnowledgePointId());
            item.put("abilityPointId", delta.getAbilityPointId());
            item.put("deltaScore", delta.getDeltaScore());
            item.put("beforeScore", delta.getBeforeScore());
            item.put("afterScore", delta.getAfterScore());
            item.put("reason", delta.getReason());
            item.put("aiSummary", delta.getAiSummary());
            item.put("createdAt", delta.getCreatedAt());
            return item;
        }).toList();
    }

    @Override
    public Map<String, Object> getAttemptReport(String studentNo, String evaluationId) {
        StudentTowerAttempt attempt = attemptMapper.selectById(evaluationId);
        if (attempt == null || !studentNo.equals(attempt.getStudentNo())) {
            throw new IllegalArgumentException("评价记录不存在");
        }
        return reportFromAttempt(attempt);
    }

    private List<StudentTowerNode> planNodes(StudentTowerRun run, String studentNo, String courseCode) {
        List<KnowledgePoint> knowledgePoints = knowledgePoints(courseCode);
        Map<String, List<String>> abilityByKp = abilityByKnowledgePoint(knowledgePoints);
        Map<String, Integer> mastery = masteryIndex(studentNo, courseCode);
        run.setRouteSource("template");
        return fallbackNodes(run, knowledgePoints, abilityByKp, mastery);
    }

    private List<StudentTowerNode> aiPlannedNodes(StudentTowerRun run, String studentNo, String courseCode,
                                                 List<KnowledgePoint> knowledgePoints,
                                                 Map<String, List<String>> abilityByKp,
                                                 Map<String, Integer> mastery) {
        try {
            AgenticRequest request = new AgenticRequest();
            request.setCourseCode(courseCode);
            request.setContent("Generate a personalized tower route as compact JSON.");
            request.setContext(Map.of(
                    "studentNo", studentNo,
                    "knowledgePoints", knowledgePoints.stream().map(kp -> Map.of(
                            "knowledgePointId", kp.getKnowledgePointId(),
                            "name", kp.getName(),
                            "importance", kp.getImportance() == null ? 1 : kp.getImportance(),
                            "mastery", mastery.getOrDefault(kp.getKnowledgePointId(), 0),
                            "abilityPointIds", abilityByKp.getOrDefault(kp.getKnowledgePointId(), List.of())
                    )).toList(),
                    "constraints", Map.of("minBattleRooms", 3, "maxBattleRooms", 8, "allowAuxiliaryRooms", true)
            ));
            AgenticResponse response = agenticClient.invoke("tower-route-plan", request);
            if (response == null || !response.isSuccess()) return List.of();
            String raw = firstText(response.getData() == null ? null : response.getData().get("answer"),
                    response.getData() == null ? null : response.getData().get("result"),
                    response.getData() == null ? null : response.getData().get("content"));
            if (raw == null) return List.of();
            JsonNode root = objectMapper.readTree(extractJson(raw));
            JsonNode nodes = root.path("nodes");
            if (!nodes.isArray() || nodes.isEmpty()) return List.of();
            List<StudentTowerNode> result = nodesFromAi(run, nodes, knowledgePoints, abilityByKp);
            if (result.stream().noneMatch(node -> BATTLE_TYPES.contains(node.getRoomType()))) return List.of();
            run.setAiSnapshotJson(raw);
            return result;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<StudentTowerNode> nodesFromAi(StudentTowerRun run, JsonNode nodes,
                                               List<KnowledgePoint> knowledgePoints,
                                               Map<String, List<String>> abilityByKp) {
        Set<String> validKp = new LinkedHashSet<>();
        knowledgePoints.forEach(kp -> validKp.add(kp.getKnowledgePointId()));
        List<StudentTowerNode> result = new ArrayList<>();
        Map<String, String> battleByKp = new HashMap<>();
        String previousMain = null;
        int order = 1;
        for (JsonNode item : nodes) {
            String roomType = normalizeRoomType(item.path("roomType").asText("battle"));
            String kpId = item.path("knowledgePointId").asText("");
            if (!validKp.contains(kpId)) kpId = knowledgePoints.isEmpty() ? "" : knowledgePoints.get(0).getKnowledgePointId();
            StudentTowerNode node = newNode(run.getRunId(), order++, item.path("row").asInt(order),
                    item.path("col").asInt(1), roomType, kpId, firstAbility(kpId, abilityByKp),
                    item.path("difficulty").asInt(1), item.path("reason").asText(""));
            if (result.isEmpty()) {
                node.setStatus("available");
            } else if (BATTLE_TYPES.contains(roomType) || "diagnosis".equals(roomType)) {
                node.setUnlockAfterNodeId(previousMain);
                node.setStatus("locked");
                previousMain = node.getNodeId();
            } else {
                String parent = battleByKp.getOrDefault(kpId, previousMain);
                node.setParentNodeId(parent);
                node.setUnlockAfterNodeId(parent);
                node.setStatus("locked");
            }
            if (BATTLE_TYPES.contains(roomType)) battleByKp.put(kpId, node.getNodeId());
            if (previousMain == null && (BATTLE_TYPES.contains(roomType) || "diagnosis".equals(roomType))) {
                previousMain = node.getNodeId();
            }
            result.add(node);
        }
        ensureDiagnosisBindings(result);
        return result;
    }

    private List<StudentTowerNode> fallbackNodes(StudentTowerRun run, List<KnowledgePoint> knowledgePoints,
                                                 Map<String, List<String>> abilityByKp,
                                                 Map<String, Integer> mastery) {
        List<KnowledgePoint> selected = new ArrayList<>(knowledgePoints);
        selected.sort(Comparator
                .comparingInt((KnowledgePoint kp) -> mastery.getOrDefault(kp.getKnowledgePointId(), 0))
                .thenComparing(kp -> kp.getImportance() == null ? 9 : -kp.getImportance()));
        if (selected.size() > 8) selected = new ArrayList<>(selected.subList(0, 8));
        if (selected.isEmpty()) return List.of();

        List<StudentTowerNode> nodes = new ArrayList<>();
        String previousMain = null;
        int order = 1;
        int row = 1;
        for (int i = 0; i < selected.size(); i++) {
            KnowledgePoint kp = selected.get(i);
            String kpId = kp.getKnowledgePointId();
            int score = mastery.getOrDefault(kpId, 0);
            boolean needsDiagnosis = i == 0 || score < 60;
            StudentTowerNode diagnosis = null;
            if (needsDiagnosis) {
                diagnosis = newNode(run.getRunId(), order++, row++, 1, "diagnosis", kpId,
                        firstAbility(kpId, abilityByKp), 1, score < 60 ? "掌握度偏低，先进行诊断。" : "路线起点诊断。");
                diagnosis.setStatus(nodes.isEmpty() ? "available" : "locked");
                diagnosis.setUnlockAfterNodeId(previousMain);
                nodes.add(diagnosis);
                previousMain = diagnosis.getNodeId();
            }

            String battleType = i == selected.size() - 1 ? "boss" : needsDiagnosis || score < 40 ? "elite" : "battle";
            StudentTowerNode battle = newNode(run.getRunId(), order++, row++, 1, battleType, kpId,
                    firstAbility(kpId, abilityByKp), battleType.equals("elite") ? 2 : battleType.equals("boss") ? 3 : 1,
                    "根据当前掌握度安排的主线挑战。");
            battle.setStatus(nodes.isEmpty() ? "available" : "locked");
            battle.setUnlockAfterNodeId(previousMain);
            nodes.add(battle);
            if (diagnosis != null) diagnosis.setParentNodeId(battle.getNodeId());
            previousMain = battle.getNodeId();

            if (i % 2 == 0 && i < selected.size() - 1) {
                StudentTowerNode rest = newNode(run.getRunId(), order++, row - 1, 2, "rest", kpId,
                        firstAbility(kpId, abilityByKp), 1, "通关后提供复盘与恢复。");
                rest.setParentNodeId(battle.getNodeId());
                rest.setUnlockAfterNodeId(battle.getNodeId());
                rest.setStatus("locked");
                nodes.add(rest);
            } else if (i < selected.size() - 1) {
                StudentTowerNode shop = newNode(run.getRunId(), order++, row - 1, 2, "shop", kpId,
                        firstAbility(kpId, abilityByKp), 1, "通关后提供提示和补给购买。");
                shop.setParentNodeId(battle.getNodeId());
                shop.setUnlockAfterNodeId(battle.getNodeId());
                shop.setStatus("locked");
                nodes.add(shop);
            }
        }
        return nodes;
    }

    private void ensureDiagnosisBindings(List<StudentTowerNode> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            StudentTowerNode node = nodes.get(i);
            if (!"diagnosis".equals(node.getRoomType()) || node.getParentNodeId() != null) continue;
            for (int j = i + 1; j < nodes.size(); j++) {
                StudentTowerNode candidate = nodes.get(j);
                if (BATTLE_TYPES.contains(candidate.getRoomType())
                        && Objects.equals(node.getKnowledgePointId(), candidate.getKnowledgePointId())) {
                    node.setParentNodeId(candidate.getNodeId());
                    break;
                }
            }
        }
    }

    private void applyCompletion(StudentTowerRun run, StudentTowerNode node, String result, boolean cleared,
                                 double correctRate, int hpLeft) {
        LocalDateTime now = LocalDateTime.now();
        if (cleared) {
            node.setStatus("cleared");
            node.setClearedAt(now);
        } else if (BATTLE_TYPES.contains(node.getRoomType())) {
            node.setStatus("failed");
        } else {
            node.setStatus("cleared");
            node.setClearedAt(now);
            cleared = true;
        }
        node.setUpdatedAt(now);
        nodeMapper.updateById(node);

        if (cleared) unlockChildren(run, node);
        if (cleared && node.getKnowledgePointId() != null && !node.getKnowledgePointId().isBlank()) {
            floorProgressService.updateFloorStatus(run.getStudentNo(), run.getCourseCode(),
                    node.getKnowledgePointId(), "cleared");
        }
        if (!cleared && node.getKnowledgePointId() != null && !node.getKnowledgePointId().isBlank()) {
            floorProgressService.updateFloorStatus(run.getStudentNo(), run.getCourseCode(),
                    node.getKnowledgePointId(), "weak");
        }

        publishCompletionEvent(run, node, result, cleared, correctRate, hpLeft);
        updateCurrentNode(run);
    }

    private void unlockChildren(StudentTowerRun run, StudentTowerNode node) {
        List<StudentTowerNode> children = nodeMapper.selectList(new LambdaQueryWrapper<StudentTowerNode>()
                .eq(StudentTowerNode::getRunId, run.getRunId())
                .and(q -> q.eq(StudentTowerNode::getUnlockAfterNodeId, node.getNodeId())
                        .or().eq(StudentTowerNode::getParentNodeId, node.getNodeId())));
        LocalDateTime now = LocalDateTime.now();
        for (StudentTowerNode child : children) {
            if ("locked".equals(child.getStatus())) {
                child.setStatus("available");
                child.setUpdatedAt(now);
                nodeMapper.updateById(child);
            }
        }
    }

    private void updateCurrentNode(StudentTowerRun run) {
        List<StudentTowerNode> nodes = nodes(run.getRunId());
        String next = nodes.stream()
                .filter(node -> "available".equals(node.getStatus()) || "failed".equals(node.getStatus()))
                .sorted(Comparator.comparing(StudentTowerNode::getNodeOrder))
                .map(StudentTowerNode::getNodeId)
                .findFirst()
                .orElse(null);
        run.setCurrentNodeId(next);
        run.setStatus(next == null ? "completed" : "active");
        run.setUpdatedAt(LocalDateTime.now());
        runMapper.updateById(run);
    }

    private void publishCompletionEvent(StudentTowerRun run, StudentTowerNode node, String result,
                                        boolean cleared, double correctRate, int hpLeft) {
        String eventType;
        if (!cleared) {
            eventType = GameEventTypes.FLOOR_FAILED;
        } else if ("boss".equals(node.getRoomType())) {
            eventType = GameEventTypes.BOSS_DEFEATED;
        } else if ("elite".equals(node.getRoomType())) {
            eventType = GameEventTypes.ELITE_DEFEATED;
        } else if ("rest".equals(node.getRoomType())) {
            eventType = GameEventTypes.REST_TAKEN;
        } else if ("shop".equals(node.getRoomType())) {
            eventType = GameEventTypes.SHOP_PURCHASED;
        } else if ("treasure".equals(node.getRoomType())) {
            eventType = GameEventTypes.TREASURE_OPENED;
        } else {
            eventType = GameEventTypes.FLOOR_CLEARED;
        }
        eventPublisher.publish(GameEvent.builder()
                .eventId(SharedIds.newId())
                .eventType(eventType)
                .studentId(run.getStudentNo())
                .courseId(run.getCourseCode())
                .sourceId(node.getNodeId())
                .occurredAt(LocalDateTime.now())
                .payload(Map.of(
                        "run_id", run.getRunId(),
                        "node_id", node.getNodeId(),
                        "knowledge_point_id", safe(node.getKnowledgePointId()),
                        "ability_point_id", safe(node.getAbilityPointId()),
                        "room_type", node.getRoomType(),
                        "result", result,
                        "correct_rate", correctRate,
                        "hp_left", hpLeft))
                .build());
    }

    private Map<String, Object> buildTowerDiagnosisReport(StudentTowerRun run, StudentTowerNode node,
                                                          Map<String, Object> request, double correctRate,
                                                          boolean cleared, String sourceStage) {
        List<Map<String, Object>> answers = answerList(request);
        List<Map<String, Object>> validAnswers = validReportAnswers(answers, expectedAnswerSources(sourceStage));
        if (validAnswers.isEmpty()) {
            return reportEnvelope("failed", null, "invalid_answer_summary",
                    "未检测到有效的战斗房答题记录，无法生成 AI 诊断。", false, false);
        }

        if (agenticClient.isMockMode()) {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("summary", "模拟 AI 诊断：当前为开发联调模式，未调用真实 AI。");
            report.put("weaknesses", cleared ? List.of() : List.of("模拟结果不能作为真实学习诊断依据"));
            report.put("recommendedAction", cleared ? "配置真实 AI 后重新生成诊断" : "配置真实 AI 后再查看针对性复盘建议");
            report.put("reviewFocus", List.of());
            report.put("source", "mock_ai");
            return reportEnvelope("mock", report, "mock_ai", null, true, true);
        }

        if (!agenticClient.isConfiguredForRealAi()) {
            return reportEnvelope("failed", null, "ai_unconfigured", agenticClient.configurationMessage(), false, true);
        }

        try {
            AgenticRequest aiRequest = new AgenticRequest();
            aiRequest.setCourseCode(run.getCourseCode());
            aiRequest.setKnowledgePointId(node.getKnowledgePointId());
            aiRequest.setContent("请基于本次爬塔答题记录生成学习诊断 JSON。");
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("studentNo", run.getStudentNo());
            context.put("courseCode", run.getCourseCode());
            context.put("knowledgePointId", safe(node.getKnowledgePointId()));
            context.put("abilityPointId", safe(node.getAbilityPointId()));
            context.put("roomType", safe(node.getRoomType()));
            context.put("stage", sourceStage);
            context.put("correctRate", correctRate);
            context.put("cleared", cleared);
            context.put("answers", validAnswers);
            context.put("wrongAnswers", wrongAnswers(validAnswers));
            context.put("questionCount", validAnswers.size());
            context.put("node", toNodeDto(node, supportIndexes(run.getCourseCode())));
            aiRequest.setContext(context);
            AgenticResponse response = agenticClient.invoke("tower-diagnosis-report", aiRequest);
            if (response != null && response.isSuccess() && response.getData() != null) {
                if (response.getData().containsKey("summary") || response.getData().containsKey("weaknesses")) {
                    Map<String, Object> report = new LinkedHashMap<>(response.getData());
                    report.putIfAbsent("source", "real_ai");
                    return reportEnvelope("success", report, String.valueOf(report.get("source")), null, true, false);
                }
                String raw = firstText(response.getData().get("answer"), response.getData().get("result"),
                        response.getData().get("content"), response.getData().get("summary"));
                if (raw != null && raw.trim().startsWith("{")) {
                    Map<String, Object> report = objectMapper.readValue(extractJson(raw), new TypeReference<Map<String, Object>>() {});
                    report.putIfAbsent("source", "real_ai");
                    return reportEnvelope("success", report, String.valueOf(report.get("source")), null, true, false);
                }
                if (raw != null) {
                    Map<String, Object> report = new LinkedHashMap<>();
                    report.put("summary", raw);
                    report.put("weaknesses", List.of());
                    report.put("recommendedAction", cleared ? "返回地图继续推进" : "复盘错题后重试本节点");
                    report.put("reviewFocus", List.of());
                    report.put("source", "ai_text");
                    return reportEnvelope("success", report, "ai_text", null, true, false);
                }
            }
            return reportEnvelope("failed", null, "ai_empty_response", "AI 未返回有效诊断内容", false, true);
        } catch (Exception e) {
            return reportEnvelope("failed", null, "ai_error", "AI 诊断生成失败：" + e.getMessage(), false, true);
        }
    }

    private Map<String, Object> reportEnvelope(String status, Map<String, Object> report, String source,
                                               String errorMessage, boolean aiAvailable, boolean retryable) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("aiReportStatus", status);
        envelope.put("aiAvailable", aiAvailable);
        envelope.put("reportSource", source);
        envelope.put("errorMessage", errorMessage);
        envelope.put("retryable", retryable);
        envelope.put("diagnosis", report);
        envelope.put("report", report);
        return envelope;
    }

    private RateStats calculateBattleCorrectRate(Map<String, Object> request, double fallback, Set<String> acceptedSources) {
        List<Map<String, Object>> answers = answerList(request);
        int graded = 0;
        int correct = 0;
        for (Map<String, Object> answer : answers) {
            String source = String.valueOf(answer.getOrDefault("source", "battle_room"));
            if (!acceptedSources.contains(source)) continue;
            boolean autoGradable = answer.containsKey("autoGradable")
                    ? Boolean.TRUE.equals(answer.get("autoGradable"))
                    : isAutoGradableType(String.valueOf(answer.get("type")));
            boolean answered = answer.containsKey("answered")
                    ? Boolean.TRUE.equals(answer.get("answered"))
                    : String.valueOf(answer.getOrDefault("studentAnswer", "")).trim().length() > 0;
            if (!answered) continue;
            if (!autoGradable) continue;
            graded++;
            if (Boolean.TRUE.equals(answer.get("correct"))) correct++;
        }
        if (graded <= 0) {
            return new RateStats(Math.max(0D, Math.min(1D, fallback)), 0, 0, "client_fallback");
        }
        return new RateStats(correct / (double) graded, correct, graded, "answer_summary");
    }

    private String reportStageFor(StudentTowerNode node) {
        if ("boss".equals(node.getRoomType())) return "boss_room";
        if ("elite".equals(node.getRoomType())) return "elite_room";
        return "battle_room";
    }

    private Set<String> expectedAnswerSources(String sourceStage) {
        if ("elite_room".equals(sourceStage)) return Set.of("elite_room", "battle_room");
        if ("boss_room".equals(sourceStage)) return Set.of("boss_room");
        if ("diagnosis_room".equals(sourceStage)) return Set.of("diagnosis_room");
        return Set.of("battle_room");
    }

    private List<Map<String, Object>> validReportAnswers(List<Map<String, Object>> answers, Set<String> acceptedSources) {
        return answers.stream()
                .filter(answer -> acceptedSources.contains(String.valueOf(answer.getOrDefault("source", "battle_room"))))
                .filter(answer -> answer.containsKey("answered")
                        ? Boolean.TRUE.equals(answer.get("answered"))
                        : String.valueOf(answer.getOrDefault("studentAnswer", "")).trim().length() > 0)
                .filter(answer -> answer.containsKey("autoGradable")
                        ? Boolean.TRUE.equals(answer.get("autoGradable"))
                        : isAutoGradableType(String.valueOf(answer.get("type"))))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> answerList(Map<String, Object> request) {
        Object raw = request.getOrDefault("answerSummary", request.getOrDefault("answers", List.of()));
        if (!(raw instanceof List<?> list)) return List.of();
        List<Map<String, Object>> answers = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> answer = new LinkedHashMap<>();
                map.forEach((key, value) -> answer.put(String.valueOf(key), value));
                answers.add(answer);
            }
        }
        return answers;
    }

    private String evaluationId(Map<String, Object> request, String studentNo,
                                StudentTowerRun run, StudentTowerNode node) {
        String evaluationId = stringValue(request, "evaluationId");
        if (evaluationId == null || evaluationId.isBlank()) evaluationId = SharedIds.newId();
        abilitySnapshotService.createBeforeSnapshots(evaluationId, studentNo, run.getCourseCode(),
                run.getRunId(), node.getNodeId());
        return evaluationId;
    }

    private Set<String> allowedQuestionIds(Map<String, Object> request, String studentNo,
                                           StudentTowerRun run, StudentTowerNode node) {
        String packId = stringValue(request, "packId");
        if (packId == null || packId.isBlank()) return Set.of();
        StudentTowerQuestionPack pack = questionPackMapper.selectById(packId);
        if (pack == null || !studentNo.equals(pack.getStudentNo())
                || !run.getRunId().equals(pack.getRunId()) || !node.getNodeId().equals(pack.getNodeId())
                || !run.getCourseCode().equals(pack.getCourseCode())) {
            throw new IllegalArgumentException("题包与当前评价不匹配");
        }
        try {
            return new LinkedHashSet<>(objectMapper.readValue(pack.getQuestionIdsJson(), new TypeReference<List<String>>() {}));
        } catch (Exception e) {
            throw new IllegalStateException("题包数据损坏", e);
        }
    }

    private Map<String, Object> completedResponse(StudentTowerRun run, StudentTowerNode node,
                                                  StudentTowerAttempt attempt) {
        Map<String, Object> response = new LinkedHashMap<>(toRunDto(runMapper.selectById(run.getRunId()), nodes(run.getRunId())));
        double correctRate = attempt.getCorrectRate() == null ? 0D : attempt.getCorrectRate().doubleValue();
        response.put("evaluationId", attempt.getAttemptId());
        response.put("correctRate", correctRate);
        response.put("battleCorrectRate", correctRate);
        response.put("cleared", isClearedResult(attempt.getResult()));
        response.put("correctRateSource", "server_evidence_replay");
        response.put("abilityRadar", abilityRadarService.getAbilityRadar(run.getStudentNo(), run.getCourseCode(),
                run.getRunId(), node.getNodeId()));
        response.putAll(reportFromAttempt(attempt));
        return response;
    }

    private Map<String, Object> reportFromAttempt(StudentTowerAttempt attempt) {
        Map<String, Object> report = readJsonMap(attempt.getAiReportJson());
        return report.isEmpty() ? reportEnvelope("pending", null, "async", null, true, false) : report;
    }

    private List<Map<String, Object>> verifiedAnswerSummary(
            List<Map<String, Object>> submitted,
            List<LearningEvidenceService.AnswerResult> verified) {
        Map<String, LearningEvidenceService.AnswerResult> resultIndex = new HashMap<>();
        verified.forEach(result -> resultIndex.put(result.questionId(), result));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : submitted) {
            String questionId = stringValue(item, "questionId");
            LearningEvidenceService.AnswerResult serverResult = resultIndex.get(questionId);
            if (serverResult == null) continue;
            Map<String, Object> verifiedItem = new LinkedHashMap<>(item);
            verifiedItem.put("correct", serverResult.correct());
            verifiedItem.put("knowledgePointId", serverResult.knowledgePointId());
            verifiedItem.put("attemptNo", serverResult.attemptNo());
            verifiedItem.put("beforeMastery", serverResult.beforeMastery());
            verifiedItem.put("afterMastery", serverResult.afterMastery());
            verifiedItem.put("verifiedBy", "backend");
            result.add(verifiedItem);
        }
        return result;
    }

    private Map<String, Object> completedDiagnosisResponse(StudentTowerRun run, StudentTowerNode node,
                                                           StudentTowerAttempt attempt) {
        Map<String, Object> response = completedResponse(run, node, attempt);
        double correctRate = attempt.getCorrectRate() == null ? 0D : attempt.getCorrectRate().doubleValue();
        boolean perfect = correctRate >= 0.999D;
        response.put("status", perfect ? "perfect" : correctRate >= 0.5D ? "partial" : "weak");
        response.put("diagnosisPassed", perfect);
        response.put("run", toRunDto(runMapper.selectById(run.getRunId()), nodes(run.getRunId())));
        return response;
    }

    private List<Map<String, Object>> wrongAnswers(List<Map<String, Object>> answers) {
        return answers.stream()
                .filter(answer -> Boolean.FALSE.equals(answer.get("correct")))
                .toList();
    }

    private boolean isAutoGradableType(String type) {
        return "single".equals(type) || "multi".equals(type) || "fill".equals(type);
    }

    private static class RateStats {
        private final double correctRate;
        private final int correctCount;
        private final int gradedCount;
        private final String source;

        private RateStats(double correctRate, int correctCount, int gradedCount, String source) {
            this.correctRate = correctRate;
            this.correctCount = correctCount;
            this.gradedCount = gradedCount;
            this.source = source;
        }
    }

    private void recordAttempt(String attemptId, StudentTowerRun run, StudentTowerNode node, String studentNo, String result,
                               double correctRate, Map<String, Object> request, Map<String, Object> report) {
        StudentTowerAttempt attempt = new StudentTowerAttempt();
        attempt.setAttemptId(attemptId);
        attempt.setRunId(run.getRunId());
        attempt.setNodeId(node.getNodeId());
        attempt.setStudentNo(studentNo);
        attempt.setCourseCode(run.getCourseCode());
        attempt.setRoomType(node.getRoomType());
        attempt.setResult(result);
        attempt.setCorrectRate(BigDecimal.valueOf(Math.max(0, Math.min(1, correctRate))));
        attempt.setHpLeft(intValue(request.get("hpLeft"), intValue(request.get("hp_left"), null)));
        attempt.setAnswerSummaryJson(writeJson(request.getOrDefault("answerSummary", request.getOrDefault("answers", List.of()))));
        attempt.setAiReportJson(report == null ? null : writeJson(report));
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setFinishedAt(LocalDateTime.now());
        attemptMapper.insert(attempt);
    }

    private StudentTowerNode boundEliteNode(String runId, StudentTowerNode diagnosis) {
        if (diagnosis.getParentNodeId() != null && !diagnosis.getParentNodeId().isBlank()) {
            StudentTowerNode parent = nodeMapper.selectById(diagnosis.getParentNodeId());
            if (parent != null && BATTLE_TYPES.contains(parent.getRoomType())) return parent;
        }
        StudentTowerNode elite = nodes(runId).stream()
                .filter(node -> "elite".equals(node.getRoomType()))
                .filter(node -> Objects.equals(node.getKnowledgePointId(), diagnosis.getKnowledgePointId()))
                .findFirst()
                .orElse(null);
        if (elite != null) return elite;
        return nodes(runId).stream()
                .filter(node -> BATTLE_TYPES.contains(node.getRoomType()))
                .filter(node -> Objects.equals(node.getKnowledgePointId(), diagnosis.getKnowledgePointId()))
                .findFirst()
                .orElse(null);
    }

    private StudentTowerRun activeRun(String studentNo, String courseCode) {
        return runMapper.selectOne(new LambdaQueryWrapper<StudentTowerRun>()
                .eq(StudentTowerRun::getStudentNo, studentNo)
                .eq(StudentTowerRun::getCourseCode, courseCode)
                .eq(StudentTowerRun::getStatus, "active")
                .last("limit 1"));
    }

    private StudentTowerRun requireRun(String studentNo, String runId) {
        StudentTowerRun run = runMapper.selectById(runId);
        if (run == null || !studentNo.equals(run.getStudentNo())) throw new IllegalArgumentException("路线不存在");
        return run;
    }

    private StudentTowerNode requireNode(String runId, String nodeId) {
        StudentTowerNode node = nodeMapper.selectById(nodeId);
        if (node == null || !runId.equals(node.getRunId())) throw new IllegalArgumentException("节点不存在");
        return node;
    }

    private int nextVersion(String studentNo, String courseCode) {
        Long count = runMapper.selectCount(new LambdaQueryWrapper<StudentTowerRun>()
                .eq(StudentTowerRun::getStudentNo, studentNo)
                .eq(StudentTowerRun::getCourseCode, courseCode));
        return count == null ? 1 : count.intValue() + 1;
    }

    private List<StudentTowerNode> nodes(String runId) {
        return nodeMapper.selectList(new LambdaQueryWrapper<StudentTowerNode>()
                .eq(StudentTowerNode::getRunId, runId)
                .orderByAsc(StudentTowerNode::getNodeOrder));
    }

    private List<KnowledgePoint> knowledgePoints(String courseCode) {
        List<KnowledgePoint> points = knowledgePointService.listByCourseCode(courseCode, null);
        return points == null ? List.of() : points;
    }

    private Map<String, Integer> masteryIndex(String studentNo, String courseCode) {
        Map<String, Integer> result = new HashMap<>();
        List<KnowledgeMastery> masteries = knowledgeMasteryMapper.selectList(new LambdaQueryWrapper<KnowledgeMastery>()
                .eq(KnowledgeMastery::getStudentNo, studentNo)
                .eq(KnowledgeMastery::getCourseCode, courseCode));
        for (KnowledgeMastery mastery : masteries) {
            if (mastery.getKnowledgePointId() != null && mastery.getMasteryScore() != null) {
                result.put(mastery.getKnowledgePointId(), mastery.getMasteryScore());
            }
        }
        return result;
    }

    private Map<String, List<String>> abilityByKnowledgePoint(List<KnowledgePoint> knowledgePoints) {
        Map<String, List<String>> result = new HashMap<>();
        if (knowledgePoints.isEmpty()) return result;
        List<String> kpIds = knowledgePoints.stream().map(KnowledgePoint::getKnowledgePointId).filter(Objects::nonNull).toList();
        if (kpIds.isEmpty()) return result;
        List<AbilityKnowledgePoint> mappings = abilityKnowledgePointMapper.selectList(new LambdaQueryWrapper<AbilityKnowledgePoint>()
                .in(AbilityKnowledgePoint::getKnowledgePointId, kpIds));
        for (AbilityKnowledgePoint mapping : mappings) {
            result.computeIfAbsent(mapping.getKnowledgePointId(), key -> new ArrayList<>()).add(mapping.getAbilityPointId());
        }
        return result;
    }

    private Map<String, Object> toRunDto(StudentTowerRun run, List<StudentTowerNode> nodes) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> indexes = supportIndexes(run.getCourseCode());
        result.put("runId", run.getRunId());
        result.put("studentNo", run.getStudentNo());
        result.put("courseCode", run.getCourseCode());
        result.put("status", run.getStatus());
        result.put("routeSource", run.getRouteSource());
        result.put("currentNodeId", run.getCurrentNodeId());
        result.put("nodes", nodes.stream().map(node -> toNodeDto(node, indexes)).toList());
        return result;
    }

    private Map<String, Object> toNodeDto(StudentTowerNode node, Map<String, Object> indexes) {
        @SuppressWarnings("unchecked")
        Map<String, KnowledgePoint> kpIndex = (Map<String, KnowledgePoint>) indexes.getOrDefault("kp", Map.of());
        @SuppressWarnings("unchecked")
        Map<String, AbilityPoint> abilityIndex = (Map<String, AbilityPoint>) indexes.getOrDefault("ability", Map.of());
        KnowledgePoint kp = kpIndex.get(node.getKnowledgePointId());
        AbilityPoint ability = abilityIndex.get(node.getAbilityPointId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("nodeId", node.getNodeId());
        item.put("runId", node.getRunId());
        item.put("nodeOrder", node.getNodeOrder());
        item.put("level", node.getNodeOrder());
        item.put("row", node.getRowNo());
        item.put("col", node.getColNo());
        item.put("roomType", node.getRoomType());
        item.put("status", node.getStatus());
        item.put("floorStatus", node.getStatus());
        item.put("isAccessible", !"locked".equals(node.getStatus()) && !"disabled".equals(node.getStatus()));
        item.put("kpId", node.getKnowledgePointId());
        item.put("knowledgePointId", node.getKnowledgePointId());
        item.put("kpName", kp == null ? node.getKnowledgePointId() : kp.getName());
        item.put("knowledgePointName", kp == null ? node.getKnowledgePointId() : kp.getName());
        item.put("description", kp == null ? "" : safe(kp.getDescription()));
        item.put("abilityPointId", node.getAbilityPointId());
        item.put("abilityPointName", ability == null ? "" : ability.getName());
        item.put("parentNodeId", safe(node.getParentNodeId()));
        item.put("unlockAfterNodeId", safe(node.getUnlockAfterNodeId()));
        item.put("difficulty", node.getDifficulty());
        item.put("aiReason", safe(node.getAiReason()));
        item.put("statusReason", "tower_node:" + node.getStatus() + "; unlock_after:" + safe(node.getUnlockAfterNodeId()));
        item.put("clearedAt", node.getClearedAt());
        item.put("payload", readJsonMap(node.getPayloadJson()));
        return item;
    }

    private Map<String, Object> supportIndexes(String courseCode) {
        Map<String, KnowledgePoint> kp = new HashMap<>();
        knowledgePoints(courseCode).forEach(point -> kp.put(point.getKnowledgePointId(), point));
        Map<String, AbilityPoint> ability = new HashMap<>();
        abilityPointService.listByCourseCode(courseCode).forEach(point -> ability.put(point.getAbilityPointId(), point));
        return Map.of("kp", kp, "ability", ability);
    }

    private StudentTowerNode newNode(String runId, int order, int row, int col, String roomType,
                                     String kpId, String abilityPointId, int difficulty, String reason) {
        StudentTowerNode node = new StudentTowerNode();
        node.setNodeId(SharedIds.newId());
        node.setRunId(runId);
        node.setNodeOrder(order);
        node.setRowNo(row);
        node.setColNo(col);
        node.setRoomType(normalizeRoomType(roomType));
        node.setStatus("locked");
        node.setKnowledgePointId(kpId);
        node.setAbilityPointId(abilityPointId);
        node.setDifficulty(Math.max(1, Math.min(5, difficulty)));
        node.setAiReason(reason);
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());
        return node;
    }

    private String firstAbility(String kpId, Map<String, List<String>> abilityByKp) {
        return abilityByKp.getOrDefault(kpId, List.of()).stream().filter(Objects::nonNull).findFirst().orElse("");
    }

    private boolean isClearedResult(String result) {
        return "cleared".equals(result) || "diagnosis_perfect".equals(result)
                || "rest_taken".equals(result) || "shop_purchased".equals(result)
                || "treasure_opened".equals(result) || "event_resolved".equals(result);
    }

    private String normalizeRoomType(String roomType) {
        if (roomType == null || roomType.isBlank()) return "battle";
        String normalized = roomType.trim().toLowerCase();
        return switch (normalized) {
            case "diagnosis", "battle", "elite", "boss", "rest", "shop", "treasure", "supply", "event" -> normalized;
            default -> "battle";
        };
    }

    private String extractJson(String raw) {
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1);
        return trimmed;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private Map<String, Object> readJsonMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String firstText(Object... values) {
        for (Object value : values) {
            if (value != null && !String.valueOf(value).isBlank()) return String.valueOf(value).trim();
        }
        return null;
    }

    private String stringValue(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private double doubleValue(Object value, double fallback) {
        if (value instanceof Number number) return number.doubleValue();
        if (value == null) return fallback;
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private Integer intValue(Object value, Integer fallback) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

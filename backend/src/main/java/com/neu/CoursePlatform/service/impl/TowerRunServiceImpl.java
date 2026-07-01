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
import com.neu.CoursePlatform.entity.AbilityKnowledgePoint;
import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.entity.KnowledgeMastery;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.StudentAbilityDeltaLog;
import com.neu.CoursePlatform.entity.StudentTowerAttempt;
import com.neu.CoursePlatform.entity.StudentTowerNode;
import com.neu.CoursePlatform.entity.StudentTowerRun;
import com.neu.CoursePlatform.mapper.AbilityKnowledgePointMapper;
import com.neu.CoursePlatform.mapper.KnowledgeMasteryMapper;
import com.neu.CoursePlatform.mapper.StudentAbilityDeltaLogMapper;
import com.neu.CoursePlatform.mapper.StudentTowerAttemptMapper;
import com.neu.CoursePlatform.mapper.StudentTowerNodeMapper;
import com.neu.CoursePlatform.mapper.StudentTowerRunMapper;
import com.neu.CoursePlatform.profile.entity.CompetencyScore;
import com.neu.CoursePlatform.profile.service.ProfileService;
import com.neu.CoursePlatform.service.AbilityPointService;
import com.neu.CoursePlatform.service.FloorProgressService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.TowerRunService;
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
    private final ProfileService profileService;
    private final FloorProgressService floorProgressService;
    private final GameEventPublisher eventPublisher;
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
                               ProfileService profileService,
                               FloorProgressService floorProgressService,
                               GameEventPublisher eventPublisher,
                               AgenticClient agenticClient) {
        this.runMapper = runMapper;
        this.nodeMapper = nodeMapper;
        this.attemptMapper = attemptMapper;
        this.deltaMapper = deltaMapper;
        this.knowledgePointService = knowledgePointService;
        this.abilityPointService = abilityPointService;
        this.abilityKnowledgePointMapper = abilityKnowledgePointMapper;
        this.knowledgeMasteryMapper = knowledgeMasteryMapper;
        this.profileService = profileService;
        this.floorProgressService = floorProgressService;
        this.eventPublisher = eventPublisher;
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
        planned.sort(Comparator.comparing(StudentTowerNode::getNodeOrder));
        String firstAvailable = planned.stream()
                .filter(node -> "available".equals(node.getStatus()))
                .map(StudentTowerNode::getNodeId)
                .findFirst()
                .orElse(planned.get(0).getNodeId());
        run.setCurrentNodeId(firstAvailable);
        runMapper.insert(run);
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
    public Map<String, Object> enterNode(String studentNo, String runId, String nodeId) {
        StudentTowerRun run = requireRun(studentNo, runId);
        StudentTowerNode node = requireNode(runId, nodeId);
        if ("locked".equals(node.getStatus()) || "disabled".equals(node.getStatus())) {
            throw new IllegalStateException("节点尚未解锁");
        }
        run.setCurrentNodeId(nodeId);
        run.setUpdatedAt(LocalDateTime.now());
        runMapper.updateById(run);
        return getNode(studentNo, runId, nodeId);
    }

    @Override
    @Transactional
    public Map<String, Object> completeNode(String studentNo, String runId, String nodeId, Map<String, Object> request) {
        StudentTowerRun run = requireRun(studentNo, runId);
        StudentTowerNode node = requireNode(runId, nodeId);
        if ("locked".equals(node.getStatus())) throw new IllegalStateException("节点尚未解锁");

        String result = stringValue(request, "result");
        if (result == null || result.isBlank()) result = Boolean.TRUE.equals(request.get("cleared")) ? "cleared" : "failed";
        double correctRate = doubleValue(request.get("correctRate"), doubleValue(request.get("correct_rate"), 0D));
        boolean cleared = isClearedResult(result);
        recordAttempt(run, node, studentNo, result, correctRate, request, null);
        applyCompletion(run, node, result, cleared, correctRate, intValue(request.get("hpLeft"), intValue(request.get("hp_left"), 0)));
        return toRunDto(runMapper.selectById(runId), nodes(runId));
    }

    @Override
    @Transactional
    public Map<String, Object> diagnoseNode(String studentNo, String runId, String nodeId, Map<String, Object> request) {
        StudentTowerRun run = requireRun(studentNo, runId);
        StudentTowerNode node = requireNode(runId, nodeId);
        double correctRate = doubleValue(request.get("correctRate"), doubleValue(request.get("correct_rate"), 0D));
        if (request.get("answers") instanceof List<?> answers && !answers.isEmpty()) {
            long correct = answers.stream().filter(item -> item instanceof Map<?, ?> map && Boolean.TRUE.equals(map.get("correct"))).count();
            correctRate = correct / (double) answers.size();
        }
        boolean perfect = correctRate >= 0.999D;
        Map<String, Object> report = diagnosisReport(run, node, request, correctRate, perfect);
        recordAttempt(run, node, studentNo, perfect ? "diagnosis_perfect" : "diagnosis_partial", correctRate, request, report);
        applyCompletion(run, node, perfect ? "diagnosis_perfect" : "diagnosis_partial", true, correctRate, 0);
        StudentTowerNode battle = boundBattleNode(runId, node);
        if (perfect && battle != null && !"cleared".equals(battle.getStatus())) {
            recordAttempt(run, battle, studentNo, "diagnosis_perfect", correctRate, request, report);
            applyCompletion(run, battle, "diagnosis_perfect", true, correctRate, 0);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", perfect ? "perfect" : correctRate >= 0.5D ? "partial" : "weak");
        result.put("correctRate", correctRate);
        result.put("battleBypassed", perfect && battle != null);
        result.put("diagnosis", report);
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
        if (selected.isEmpty()) {
            KnowledgePoint kp = new KnowledgePoint();
            kp.setKnowledgePointId("demo-kp");
            kp.setName("课程基础挑战");
            selected.add(kp);
        }

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

            String battleType = i == selected.size() - 1 ? "boss" : score < 40 ? "elite" : "battle";
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
        writeAbilityDelta(run, node, result, correctRate, cleared);
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

    private void writeAbilityDelta(StudentTowerRun run, StudentTowerNode node, String result, double correctRate, boolean cleared) {
        if (node.getAbilityPointId() == null || node.getAbilityPointId().isBlank()) return;
        List<CompetencyScore> scores = profileService.getCompetencyScores(Integer.parseInt(run.getStudentNo()), Integer.parseInt(run.getCourseCode()));
        int before = scores.stream()
                .filter(score -> node.getAbilityPointId().equals(score.getAbilityPointId()))
                .map(CompetencyScore::getScore)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(50);
        int delta = cleared ? (result.startsWith("diagnosis") ? 3 : "boss".equals(node.getRoomType()) ? 8 : 5) : -2;
        if (correctRate >= 0.9D && cleared) delta += 2;
        int after = Math.max(0, Math.min(100, before + delta));
        StudentAbilityDeltaLog log = new StudentAbilityDeltaLog();
        log.setId(SharedIds.newId());
        log.setStudentNo(run.getStudentNo());
        log.setCourseCode(run.getCourseCode());
        log.setRunId(run.getRunId());
        log.setNodeId(node.getNodeId());
        log.setKnowledgePointId(node.getKnowledgePointId());
        log.setAbilityPointId(node.getAbilityPointId());
        log.setDeltaScore(delta);
        log.setBeforeScore(before);
        log.setAfterScore(after);
        log.setReason(cleared ? "通关节点带来的能力变化" : "挑战未通过，标记为待强化");
        log.setAiSummary(abilitySummary(node, delta, correctRate, result));
        log.setCreatedAt(LocalDateTime.now());
        deltaMapper.insert(log);
    }

    private String abilitySummary(StudentTowerNode node, int delta, double correctRate, String result) {
        try {
            AgenticRequest request = new AgenticRequest();
            request.setKnowledgePointId(node.getKnowledgePointId());
            request.setContext(Map.of("roomType", node.getRoomType(), "delta", delta,
                    "correctRate", correctRate, "result", result));
            AgenticResponse response = agenticClient.invoke("ability-growth-explain", request);
            if (response != null && response.isSuccess() && response.getData() != null) {
                String text = firstText(response.getData().get("answer"), response.getData().get("result"), response.getData().get("summary"));
                if (text != null) return text;
            }
        } catch (Exception ignored) {
        }
        return delta >= 0 ? "本次表现提升了相关能力掌握度。" : "本次挑战暴露出薄弱点，建议复习后重试。";
    }

    private Map<String, Object> diagnosisReport(StudentTowerRun run, StudentTowerNode node,
                                                Map<String, Object> request, double correctRate, boolean perfect) {
        try {
            AgenticRequest aiRequest = new AgenticRequest();
            aiRequest.setCourseCode(run.getCourseCode());
            aiRequest.setKnowledgePointId(node.getKnowledgePointId());
            aiRequest.setContext(Map.of("node", toNodeDto(node, supportIndexes(run.getCourseCode())),
                    "answers", request.getOrDefault("answers", List.of()),
                    "correctRate", correctRate,
                    "perfect", perfect));
            AgenticResponse response = agenticClient.invoke("tower-diagnosis-report", aiRequest);
            if (response != null && response.isSuccess() && response.getData() != null) {
                String raw = firstText(response.getData().get("answer"), response.getData().get("result"), response.getData().get("content"));
                if (raw != null && raw.trim().startsWith("{")) {
                    return objectMapper.readValue(extractJson(raw), new TypeReference<Map<String, Object>>() {});
                }
                if (raw != null) return Map.of("summary", raw, "weaknesses", List.of(), "recommendedAction", perfect ? "可跳过战斗" : "进入战斗巩固");
            }
        } catch (Exception ignored) {
        }
        return Map.of(
                "summary", perfect ? "诊断题全部答对，可以跳过当前普通战斗。" : "诊断显示仍有需要巩固的内容，请进入战斗继续练习。",
                "weaknesses", perfect ? List.of() : List.of("诊断中存在错误或不确定题目"),
                "recommendedAction", perfect ? "进入下一节点" : "进入战斗巩固",
                "abilityDeltas", perfect ? List.of(Map.of("abilityPointId", safe(node.getAbilityPointId()), "delta", 3,
                        "reason", "诊断全对，小幅提升相关能力。")) : List.of()
        );
    }

    private void recordAttempt(StudentTowerRun run, StudentTowerNode node, String studentNo, String result,
                               double correctRate, Map<String, Object> request, Map<String, Object> report) {
        StudentTowerAttempt attempt = new StudentTowerAttempt();
        attempt.setAttemptId(SharedIds.newId());
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

    private StudentTowerNode boundBattleNode(String runId, StudentTowerNode diagnosis) {
        if (diagnosis.getParentNodeId() != null && !diagnosis.getParentNodeId().isBlank()) {
            StudentTowerNode parent = nodeMapper.selectById(diagnosis.getParentNodeId());
            if (parent != null && BATTLE_TYPES.contains(parent.getRoomType())) return parent;
        }
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

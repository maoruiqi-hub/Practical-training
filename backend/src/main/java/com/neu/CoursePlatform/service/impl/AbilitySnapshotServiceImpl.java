package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.common.SharedIds;
import com.neu.CoursePlatform.entity.AbilityKnowledgePoint;
import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.entity.KnowledgeMastery;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.LearningAnswerEvidence;
import com.neu.CoursePlatform.entity.StudentAbilitySnapshot;
import com.neu.CoursePlatform.mapper.AbilityKnowledgePointMapper;
import com.neu.CoursePlatform.mapper.KnowledgeMasteryMapper;
import com.neu.CoursePlatform.mapper.KnowledgePointMapper;
import com.neu.CoursePlatform.mapper.LearningAnswerEvidenceMapper;
import com.neu.CoursePlatform.mapper.StudentAbilitySnapshotMapper;
import com.neu.CoursePlatform.service.AbilityPointService;
import com.neu.CoursePlatform.service.AbilitySnapshotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AbilitySnapshotServiceImpl implements AbilitySnapshotService {
    private final AbilityPointService abilityPointService;
    private final AbilityKnowledgePointMapper mappingMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final KnowledgeMasteryMapper masteryMapper;
    private final LearningAnswerEvidenceMapper evidenceMapper;
    private final StudentAbilitySnapshotMapper snapshotMapper;
    private final ObjectMapper objectMapper;

    public AbilitySnapshotServiceImpl(AbilityPointService abilityPointService,
                                      AbilityKnowledgePointMapper mappingMapper,
                                      KnowledgePointMapper knowledgePointMapper,
                                      KnowledgeMasteryMapper masteryMapper,
                                      LearningAnswerEvidenceMapper evidenceMapper,
                                      StudentAbilitySnapshotMapper snapshotMapper,
                                      ObjectMapper objectMapper) {
        this.abilityPointService = abilityPointService;
        this.mappingMapper = mappingMapper;
        this.knowledgePointMapper = knowledgePointMapper;
        this.masteryMapper = masteryMapper;
        this.evidenceMapper = evidenceMapper;
        this.snapshotMapper = snapshotMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public List<StudentAbilitySnapshot> createBeforeSnapshots(String evaluationId, String studentNo, String courseCode,
                                                              String runId, String nodeId) {
        List<StudentAbilitySnapshot> existing = snapshots(evaluationId, "before");
        if (!existing.isEmpty()) {
            boolean sameEvaluation = existing.stream().allMatch(item -> studentNo.equals(item.getStudentNo())
                    && courseCode.equals(item.getCourseCode()) && runId.equals(item.getRunId()) && nodeId.equals(item.getNodeId()));
            if (!sameEvaluation) throw new IllegalArgumentException("评价标识与当前节点不匹配");
            return existing;
        }
        List<StudentAbilitySnapshot> created = currentScores(studentNo, courseCode).stream()
                .map(score -> snapshot(evaluationId, studentNo, courseCode, runId, nodeId, "before", score))
                .toList();
        created.forEach(snapshotMapper::insert);
        return created;
    }

    @Override
    @Transactional
    public List<StudentAbilitySnapshot> createAfterSnapshots(String evaluationId) {
        List<StudentAbilitySnapshot> existing = snapshots(evaluationId, "after");
        if (!existing.isEmpty()) return existing;
        List<StudentAbilitySnapshot> before = snapshots(evaluationId, "before");
        if (before.isEmpty()) throw new IllegalArgumentException("评价快照不存在");
        StudentAbilitySnapshot context = before.get(0);
        Map<String, Integer> masteries = masteryIndex(context.getStudentNo(), context.getCourseCode());
        Set<String> evidenced = evidencedKnowledgePoints(context.getStudentNo(), context.getCourseCode());
        List<StudentAbilitySnapshot> created = new ArrayList<>();
        for (StudentAbilitySnapshot frozen : before) {
            List<String> knowledgePointIds = readStringList(frozen.getKnowledgePointIdsJson());
            List<Integer> weights = readIntegerList(frozen.getWeightsJson());
            AbilityScore score = aggregate(frozen.getAbilityPointId(), frozen.getAbilityPointName(), "",
                    knowledgePointIds, weights, masteries, evidenced);
            StudentAbilitySnapshot after = snapshot(evaluationId, context.getStudentNo(), context.getCourseCode(),
                    context.getRunId(), context.getNodeId(), "after", score);
            snapshotMapper.insert(after);
            created.add(after);
        }
        return created;
    }

    @Override
    public List<AbilityScore> currentScores(String studentNo, String courseCode) {
        List<AbilityPoint> abilities = abilityPointService.listByCourseCode(courseCode).stream()
                .sorted(Comparator.comparing(AbilityPoint::getAbilityPointId, this::compareIds))
                .toList();
        if (abilities.isEmpty()) return List.of();
        Set<String> abilityIds = abilities.stream().map(AbilityPoint::getAbilityPointId).collect(HashSet::new, Set::add, Set::addAll);
        List<AbilityKnowledgePoint> mappings = mappingMapper.selectList(new LambdaQueryWrapper<AbilityKnowledgePoint>()
                .in(AbilityKnowledgePoint::getAbilityPointId, abilityIds));
        Map<String, List<String>> knowledgeByAbility = new HashMap<>();
        Set<String> knowledgeIds = new HashSet<>();
        for (AbilityKnowledgePoint mapping : mappings) {
            knowledgeByAbility.computeIfAbsent(mapping.getAbilityPointId(), ignored -> new ArrayList<>())
                    .add(mapping.getKnowledgePointId());
            knowledgeIds.add(mapping.getKnowledgePointId());
        }
        Map<String, Integer> importance = new HashMap<>();
        if (!knowledgeIds.isEmpty()) {
            for (KnowledgePoint point : knowledgePointMapper.selectBatchIds(knowledgeIds)) {
                importance.put(point.getKnowledgePointId(), Math.max(1, point.getImportance() == null ? 1 : point.getImportance()));
            }
        }
        Map<String, Integer> masteries = masteryIndex(studentNo, courseCode);
        Set<String> evidenced = evidencedKnowledgePoints(studentNo, courseCode);
        List<AbilityScore> result = new ArrayList<>();
        for (AbilityPoint ability : abilities) {
            List<String> pointIds = knowledgeByAbility.getOrDefault(ability.getAbilityPointId(), List.of()).stream()
                    .distinct().sorted(this::compareIds).toList();
            List<Integer> weights = pointIds.stream().map(id -> importance.getOrDefault(id, 1)).toList();
            result.add(aggregate(ability.getAbilityPointId(), ability.getName(), ability.getDescription(),
                    pointIds, weights, masteries, evidenced));
        }
        return result;
    }

    private AbilityScore aggregate(String abilityPointId, String name, String description,
                                   List<String> knowledgePointIds, List<Integer> weights,
                                   Map<String, Integer> masteries, Set<String> evidenced) {
        if (knowledgePointIds.isEmpty()) {
            return new AbilityScore(abilityPointId, name, description, 50, 0, 0, List.of(), List.of());
        }
        long weightedSum = 0;
        int totalWeight = 0;
        int evidenceCount = 0;
        for (int i = 0; i < knowledgePointIds.size(); i++) {
            String pointId = knowledgePointIds.get(i);
            int weight = i < weights.size() ? Math.max(1, weights.get(i)) : 1;
            weightedSum += (long) masteries.getOrDefault(pointId, 50) * weight;
            totalWeight += weight;
            if (evidenced.contains(pointId)) evidenceCount++;
        }
        int score = totalWeight == 0 ? 50 : (int) Math.round(weightedSum / (double) totalWeight);
        return new AbilityScore(abilityPointId, name, description, score, evidenceCount,
                knowledgePointIds.size(), List.copyOf(knowledgePointIds), List.copyOf(weights));
    }

    private StudentAbilitySnapshot snapshot(String evaluationId, String studentNo, String courseCode,
                                            String runId, String nodeId, String phase, AbilityScore score) {
        StudentAbilitySnapshot snapshot = new StudentAbilitySnapshot();
        snapshot.setSnapshotId(SharedIds.newId());
        snapshot.setEvaluationId(evaluationId);
        snapshot.setStudentNo(studentNo);
        snapshot.setCourseCode(courseCode);
        snapshot.setRunId(runId);
        snapshot.setNodeId(nodeId);
        snapshot.setPhase(phase);
        snapshot.setAbilityPointId(score.abilityPointId());
        snapshot.setAbilityPointName(score.name());
        snapshot.setScore(score.score());
        snapshot.setEvidenceKnowledgeCount(score.evidenceKnowledgeCount());
        snapshot.setTotalKnowledgeCount(score.totalKnowledgeCount());
        snapshot.setKnowledgePointIdsJson(writeJson(score.knowledgePointIds()));
        snapshot.setWeightsJson(writeJson(score.weights()));
        snapshot.setCreatedAt(LocalDateTime.now());
        return snapshot;
    }

    private Map<String, Integer> masteryIndex(String studentNo, String courseCode) {
        Map<String, Integer> result = new HashMap<>();
        masteryMapper.selectList(new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getStudentNo, studentNo)
                        .eq(KnowledgeMastery::getCourseCode, courseCode))
                .forEach(item -> result.put(item.getKnowledgePointId(), item.getMasteryScore()));
        return result;
    }

    private Set<String> evidencedKnowledgePoints(String studentNo, String courseCode) {
        Set<String> result = new HashSet<>();
        evidenceMapper.selectList(new LambdaQueryWrapper<LearningAnswerEvidence>()
                        .select(LearningAnswerEvidence::getKnowledgePointId)
                        .eq(LearningAnswerEvidence::getStudentNo, studentNo)
                        .eq(LearningAnswerEvidence::getCourseCode, courseCode))
                .forEach(item -> result.add(item.getKnowledgePointId()));
        return result;
    }

    private List<StudentAbilitySnapshot> snapshots(String evaluationId, String phase) {
        return snapshotMapper.selectList(new LambdaQueryWrapper<StudentAbilitySnapshot>()
                .eq(StudentAbilitySnapshot::getEvaluationId, evaluationId)
                .eq(StudentAbilitySnapshot::getPhase, phase)
                .orderByAsc(StudentAbilitySnapshot::getAbilityPointId));
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("快照知识点数据损坏", e);
        }
    }

    private List<Integer> readIntegerList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("快照权重数据损坏", e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("无法保存能力快照", e);
        }
    }

    private int compareIds(String first, String second) {
        try {
            return Long.compare(Long.parseLong(first), Long.parseLong(second));
        } catch (Exception ignored) {
            return String.valueOf(first).compareTo(String.valueOf(second));
        }
    }
}

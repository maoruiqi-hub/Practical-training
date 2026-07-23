package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.CoursePlatform.entity.StudentAbilitySnapshot;
import com.neu.CoursePlatform.mapper.StudentAbilitySnapshotMapper;
import com.neu.CoursePlatform.service.AbilityRadarService;
import com.neu.CoursePlatform.service.AbilitySnapshotService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AbilityRadarServiceImpl implements AbilityRadarService {
    private final AbilitySnapshotService snapshotService;
    private final StudentAbilitySnapshotMapper snapshotMapper;

    public AbilityRadarServiceImpl(AbilitySnapshotService snapshotService,
                                   StudentAbilitySnapshotMapper snapshotMapper) {
        this.snapshotService = snapshotService;
        this.snapshotMapper = snapshotMapper;
    }

    @Override
    public Map<String, Object> getAbilityRadar(String studentNo, String courseCode, String runId, String nodeId) {
        String evaluationId = latestEvaluationId(studentNo, courseCode, runId, nodeId);
        if (evaluationId == null) return currentRadar(studentNo, courseCode, runId, nodeId);

        List<StudentAbilitySnapshot> before = snapshots(evaluationId, "before");
        List<StudentAbilitySnapshot> after = snapshots(evaluationId, "after");
        if (after.isEmpty()) after = before;
        Map<String, StudentAbilitySnapshot> afterIndex = new HashMap<>();
        after.forEach(item -> afterIndex.put(item.getAbilityPointId(), item));

        List<Map<String, Object>> dimensions = new ArrayList<>();
        List<Integer> beforeSeries = new ArrayList<>();
        List<Integer> afterSeries = new ArrayList<>();
        int changed = 0;
        String topChange = "";
        int largestDelta = 0;
        for (StudentAbilitySnapshot previous : before) {
            StudentAbilitySnapshot current = afterIndex.getOrDefault(previous.getAbilityPointId(), previous);
            int beforeScore = clamp(previous.getScore());
            int afterScore = clamp(current.getScore());
            int delta = afterScore - beforeScore;
            if (delta != 0) {
                changed++;
                if (Math.abs(delta) > largestDelta) {
                    largestDelta = Math.abs(delta);
                    topChange = previous.getAbilityPointName();
                }
            }
            dimensions.add(dimension(previous.getAbilityPointId(), previous.getAbilityPointName(), beforeScore,
                    afterScore, delta, current.getEvidenceKnowledgeCount(), current.getTotalKnowledgeCount()));
            beforeSeries.add(beforeScore);
            afterSeries.add(afterScore);
        }
        return radarResult("node", runId, nodeId, evaluationId, dimensions, beforeSeries, afterSeries,
                changed == 0 ? "本次评价后能力得分没有变化。"
                        : "本次共有 " + changed + " 个能力维度发生变化，主要变化集中在" + topChange + "。");
    }

    private Map<String, Object> currentRadar(String studentNo, String courseCode, String runId, String nodeId) {
        List<Map<String, Object>> dimensions = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        for (AbilitySnapshotService.AbilityScore score : snapshotService.currentScores(studentNo, courseCode)) {
            dimensions.add(dimension(score.abilityPointId(), score.name(), score.score(), score.score(), 0,
                    score.evidenceKnowledgeCount(), score.totalKnowledgeCount()));
            scores.add(score.score());
        }
        return radarResult("current", runId, nodeId, null, dimensions, scores, scores,
                "当前展示由知识点掌握度聚合得出的能力水平。");
    }

    private String latestEvaluationId(String studentNo, String courseCode, String runId, String nodeId) {
        if (nodeId == null || nodeId.isBlank()) return null;
        LambdaQueryWrapper<StudentAbilitySnapshot> query = new LambdaQueryWrapper<StudentAbilitySnapshot>()
                .eq(StudentAbilitySnapshot::getStudentNo, studentNo)
                .eq(StudentAbilitySnapshot::getCourseCode, courseCode)
                .eq(StudentAbilitySnapshot::getNodeId, nodeId)
                .eq(StudentAbilitySnapshot::getPhase, "after")
                .orderByDesc(StudentAbilitySnapshot::getCreatedAt)
                .last("LIMIT 1");
        if (runId != null && !runId.isBlank()) query.eq(StudentAbilitySnapshot::getRunId, runId);
        StudentAbilitySnapshot latest = snapshotMapper.selectOne(query);
        return latest == null ? null : latest.getEvaluationId();
    }

    private List<StudentAbilitySnapshot> snapshots(String evaluationId, String phase) {
        return snapshotMapper.selectList(new LambdaQueryWrapper<StudentAbilitySnapshot>()
                .eq(StudentAbilitySnapshot::getEvaluationId, evaluationId)
                .eq(StudentAbilitySnapshot::getPhase, phase)
                .orderByAsc(StudentAbilitySnapshot::getAbilityPointId));
    }

    private Map<String, Object> dimension(String abilityPointId, String name, int before, int after, int delta,
                                          Integer evidenceCount, Integer totalCount) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("abilityPointId", abilityPointId);
        item.put("name", name);
        item.put("beforeScore", before);
        item.put("afterScore", after);
        item.put("delta", delta);
        item.put("evidenceKnowledgeCount", evidenceCount == null ? 0 : evidenceCount);
        item.put("totalKnowledgeCount", totalCount == null ? 0 : totalCount);
        item.put("coverageRate", totalCount == null || totalCount == 0 ? 0
                : Math.round((evidenceCount == null ? 0 : evidenceCount) * 100D / totalCount));
        item.put("reason", delta == 0 ? "本次无有效分数变化" : "由本次答题证据和知识点掌握度变化聚合得出");
        return item;
    }

    private Map<String, Object> radarResult(String mode, String runId, String nodeId, String evaluationId,
                                            List<Map<String, Object>> dimensions, List<Integer> before,
                                            List<Integer> after, String summary) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", mode);
        result.put("runId", runId);
        result.put("nodeId", nodeId);
        result.put("evaluationId", evaluationId);
        result.put("dimensions", dimensions);
        result.put("series", Map.of("before", before, "after", after));
        result.put("summary", summary);
        return result;
    }

    private int clamp(Integer value) {
        return Math.max(0, Math.min(100, value == null ? 50 : value));
    }
}

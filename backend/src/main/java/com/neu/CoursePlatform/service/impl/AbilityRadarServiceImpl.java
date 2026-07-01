package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.entity.StudentAbilityDeltaLog;
import com.neu.CoursePlatform.mapper.StudentAbilityDeltaLogMapper;
import com.neu.CoursePlatform.profile.entity.CompetencyScore;
import com.neu.CoursePlatform.profile.service.ProfileService;
import com.neu.CoursePlatform.service.AbilityPointService;
import com.neu.CoursePlatform.service.AbilityRadarService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AbilityRadarServiceImpl implements AbilityRadarService {
    private final AbilityPointService abilityPointService;
    private final ProfileService profileService;
    private final StudentAbilityDeltaLogMapper deltaMapper;

    public AbilityRadarServiceImpl(AbilityPointService abilityPointService,
                                   ProfileService profileService,
                                   StudentAbilityDeltaLogMapper deltaMapper) {
        this.abilityPointService = abilityPointService;
        this.profileService = profileService;
        this.deltaMapper = deltaMapper;
    }

    @Override
    public Map<String, Object> getAbilityRadar(String studentNo, String courseCode, String runId, String nodeId) {
        List<AbilityPoint> abilities = abilityPointService.listByCourseCode(courseCode);
        Map<String, Integer> currentScores = currentScores(studentNo, courseCode);
        Map<String, List<StudentAbilityDeltaLog>> deltas = deltaIndex(studentNo, courseCode, runId, nodeId);

        List<Map<String, Object>> dimensions = new ArrayList<>();
        List<Integer> beforeSeries = new ArrayList<>();
        List<Integer> afterSeries = new ArrayList<>();
        int changed = 0;
        int maxAbsDelta = 0;
        String topChange = "";

        for (AbilityPoint ability : abilities) {
            String abilityId = ability.getAbilityPointId();
            List<StudentAbilityDeltaLog> logs = deltas.getOrDefault(abilityId, List.of());
            int current = clamp(currentScores.getOrDefault(abilityId, 50));
            int before = logs.isEmpty() || logs.get(0).getBeforeScore() == null
                    ? current
                    : clamp(logs.get(0).getBeforeScore());
            int after = logs.isEmpty() || logs.get(logs.size() - 1).getAfterScore() == null
                    ? current
                    : clamp(logs.get(logs.size() - 1).getAfterScore());
            int delta = after - before;
            if (delta != 0) {
                changed++;
                if (topChange.isBlank() || Math.abs(delta) > maxAbsDelta) {
                    topChange = ability.getName();
                    maxAbsDelta = Math.abs(delta);
                }
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("abilityPointId", abilityId);
            item.put("name", ability.getName());
            item.put("description", ability.getDescription());
            item.put("beforeScore", before);
            item.put("afterScore", after);
            item.put("delta", delta);
            item.put("reason", logs.isEmpty() ? "" : firstNonBlank(logs.get(logs.size() - 1).getAiSummary(), logs.get(logs.size() - 1).getReason()));
            dimensions.add(item);
            beforeSeries.add(before);
            afterSeries.add(after);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", nodeId != null && !nodeId.isBlank() ? "node" : runId != null && !runId.isBlank() ? "run" : "current");
        result.put("runId", runId);
        result.put("nodeId", nodeId);
        result.put("dimensions", dimensions);
        result.put("series", Map.of("before", beforeSeries, "after", afterSeries));
        result.put("summary", changed == 0
                ? "当前还没有本次通关带来的能力变化，雷达图展示的是现有能力水平。"
                : "本次通关后共有 " + changed + " 个能力维度发生变化，主要变化集中在" + topChange + "。");
        return result;
    }

    private Map<String, Integer> currentScores(String studentNo, String courseCode) {
        Map<String, Integer> result = new HashMap<>();
        try {
            List<CompetencyScore> scores = profileService.getCompetencyScores(Integer.parseInt(studentNo), Integer.parseInt(courseCode));
            for (CompetencyScore score : scores) {
                if (score.getAbilityPointId() != null && score.getScore() != null) {
                    result.put(score.getAbilityPointId(), score.getScore());
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private Map<String, List<StudentAbilityDeltaLog>> deltaIndex(String studentNo, String courseCode, String runId, String nodeId) {
        LambdaQueryWrapper<StudentAbilityDeltaLog> q = new LambdaQueryWrapper<StudentAbilityDeltaLog>()
                .eq(StudentAbilityDeltaLog::getStudentNo, studentNo)
                .eq(StudentAbilityDeltaLog::getCourseCode, courseCode)
                .orderByAsc(StudentAbilityDeltaLog::getCreatedAt);
        if (runId != null && !runId.isBlank()) q.eq(StudentAbilityDeltaLog::getRunId, runId);
        if (nodeId != null && !nodeId.isBlank()) q.eq(StudentAbilityDeltaLog::getNodeId, nodeId);
        List<StudentAbilityDeltaLog> logs = deltaMapper.selectList(q);
        Map<String, List<StudentAbilityDeltaLog>> result = new HashMap<>();
        logs.stream()
                .filter(log -> log.getAbilityPointId() != null && !log.getAbilityPointId().isBlank())
                .sorted(Comparator.comparing(StudentAbilityDeltaLog::getCreatedAt))
                .forEach(log -> result.computeIfAbsent(log.getAbilityPointId(), key -> new ArrayList<>()).add(log));
        return result;
    }

    private int clamp(Integer value) {
        return Math.max(0, Math.min(100, value == null ? 0 : value));
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second == null ? "" : second;
    }
}

*** Begin Patch
*** Add File: Practical-training-master/backend/src/main/java/com/neu/CoursePlatform/service/impl/TowerQuestionPackServiceImpl.java
package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.common.SharedIds;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.entity.StudentTowerAttempt;
import com.neu.CoursePlatform.entity.StudentTowerNode;
import com.neu.CoursePlatform.entity.StudentTowerQuestionPack;
import com.neu.CoursePlatform.entity.StudentTowerRun;
import com.neu.CoursePlatform.mapper.StudentTowerAttemptMapper;
import com.neu.CoursePlatform.mapper.StudentTowerNodeMapper;
import com.neu.CoursePlatform.mapper.StudentTowerQuestionPackMapper;
import com.neu.CoursePlatform.mapper.StudentTowerRunMapper;
import com.neu.CoursePlatform.service.QuestionService;
import com.neu.CoursePlatform.service.TowerQuestionPackService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class TowerQuestionPackServiceImpl implements TowerQuestionPackService {
    private final StudentTowerQuestionPackMapper packMapper;
    private final StudentTowerRunMapper runMapper;
    private final StudentTowerNodeMapper nodeMapper;
    private final StudentTowerAttemptMapper attemptMapper;
    private final QuestionService questionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TowerQuestionPackServiceImpl(StudentTowerQuestionPackMapper packMapper,
                                        StudentTowerRunMapper runMapper,
                                        StudentTowerNodeMapper nodeMapper,
                                        StudentTowerAttemptMapper attemptMapper,
                                        QuestionService questionService) {
        this.packMapper = packMapper;
        this.runMapper = runMapper;
        this.nodeMapper = nodeMapper;
        this.attemptMapper = attemptMapper;
        this.questionService = questionService;
    }

    @Override
    public Map<String, Object> getOrCreateQuestionPack(String studentNo, String runId, String nodeId, String mode) {
        String normalizedMode = normalizeMode(mode);
        StudentTowerQuestionPack existing = packMapper.selectOne(new LambdaQueryWrapper<StudentTowerQuestionPack>()
                .eq(StudentTowerQuestionPack::getRunId, runId)
                .eq(StudentTowerQuestionPack::getNodeId, nodeId)
                .eq(StudentTowerQuestionPack::getMode, normalizedMode)
                .last("limit 1"));
        if (existing != null) return toDto(existing);
        return regenerateQuestionPack(studentNo, runId, nodeId, normalizedMode);
    }

    @Override
    @Transactional
    public Map<String, Object> regenerateQuestionPack(String studentNo, String runId, String nodeId, String mode) {
        String normalizedMode = normalizeMode(mode);
        StudentTowerRun run = requireRun(studentNo, runId);
        StudentTowerNode node = requireNode(runId, nodeId);
        int targetCount = targetCount(normalizedMode, node.getRoomType());
        Set<String> recentUsed = recentUsedQuestionIds(studentNo, run.getCourseCode());

        List<Question> candidates = questionService
                .filterQuestions(run.getCourseCode(), null, node.getKnowledgePointId(), null, null, null);
        boolean expanded = false;
        if (candidates.size() < targetCount) {
            expanded = true;
            candidates = questionService.listByCourseCode(run.getCourseCode());
        }

        List<Question> selected = selectQuestions(candidates, node, normalizedMode, recentUsed, targetCount);
        if (selected.isEmpty()) {
            throw new IllegalStateException("当前课程题库中没有可用于该节点的题目");
        }

        packMapper.delete(new LambdaQueryWrapper<StudentTowerQuestionPack>()
                .eq(StudentTowerQuestionPack::getRunId, runId)
                .eq(StudentTowerQuestionPack::getNodeId, nodeId)
                .eq(StudentTowerQuestionPack::getMode, normalizedMode));

        StudentTowerQuestionPack pack = new StudentTowerQuestionPack();
        pack.setPackId(SharedIds.newId());
        pack.setRunId(runId);
        pack.setNodeId(nodeId);
        pack.setStudentNo(studentNo);
        pack.setCourseCode(run.getCourseCode());
        pack.setMode(normalizedMode);
        pack.setQuestionIdsJson(writeJson(selected.stream().map(Question::getQuestionId).toList()));
        pack.setSource("rule");
        pack.setStrategyJson(writeJson(Map.of(
                "targetCount", targetCount,
                "candidateCount", candidates.size(),
                "expandedToCourse", expanded,
                "knowledgePointId", safe(node.getKnowledgePointId()),
                "difficulty", node.getDifficulty() == null ? 1 : node.getDifficulty(),
                "recentUsedFiltered", recentUsed.size()
        )));
        pack.setAiReason("根据当前节点知识点、节点难度、近期做题记录和题型多样性从数据库题库中推荐。");
        pack.setCreatedAt(LocalDateTime.now());
        pack.setUpdatedAt(LocalDateTime.now());
        packMapper.insert(pack);
        return toDto(pack, selected);
    }

    private List<Question> selectQuestions(List<Question> candidates, StudentTowerNode node, String mode,
                                           Set<String> recentUsed, int targetCount) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        int targetDifficulty = Math.max(1, Math.min(5, node.getDifficulty() == null ? 1 : node.getDifficulty()));
        String kpId = safe(node.getKnowledgePointId());
        int salt = Math.abs(Objects.hash(node.getNodeId(), mode));
        List<Question> sorted = candidates.stream()
                .filter(q -> q.getQuestionId() != null)
                .sorted(Comparator
                        .comparingInt((Question q) -> -scoreQuestion(q, kpId, targetDifficulty, recentUsed, salt))
                        .thenComparing(Question::getQuestionId))
                .toList();

        LinkedHashSet<Question> selected = new LinkedHashSet<>();
        Set<String> usedTypes = new HashSet<>();
        for (Question question : sorted) {
            if (selected.size() >= targetCount) break;
            String type = safe(question.getType());
            if (usedTypes.contains(type) && selected.size() < Math.min(3, targetCount)) continue;
            selected.add(question);
            usedTypes.add(type);
        }
        for (Question question : sorted) {
            if (selected.size() >= targetCount) break;
            selected.add(question);
        }
        return new ArrayList<>(selected);
    }

    private int scoreQuestion(Question q, String kpId, int targetDifficulty, Set<String> recentUsed, int salt) {
        int score = 0;
        if (!kpId.isBlank() && kpId.equals(q.getKnowledgePointId())) score += 45;
        int difficulty = q.getDifficulty() == null ? 3 : q.getDifficulty();
        score += Math.max(0, 25 - Math.abs(difficulty - targetDifficulty) * 8);
        if (!recentUsed.contains(q.getQuestionId())) score += 18;
        String type = safe(q.getType());
        if ("single".equals(type) || "fill".equals(type)) score += 4;
        if ("multi".equals(type) || "program".equals(type) || "essay".equals(type)) score += targetDifficulty >= 2 ? 8 : 0;
        score += Math.abs(Objects.hash(q.getQuestionId(), salt)) % 11;
        return score;
    }

    private Set<String> recentUsedQuestionIds(String studentNo, String courseCode) {
        Set<String> result = new HashSet<>();
        List<StudentTowerAttempt> attempts = attemptMapper.selectList(new LambdaQueryWrapper<StudentTowerAttempt>()
                .eq(StudentTowerAttempt::getStudentNo, studentNo)
                .eq(StudentTowerAttempt::getCourseCode, courseCode)
                .orderByDesc(StudentTowerAttempt::getFinishedAt)
                .last("limit 20"));
        for (StudentTowerAttempt attempt : attempts) {
            Object parsed = readJson(attempt.getAnswerSummaryJson());
            if (parsed instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map && map.get("questionId") != null) {
                        result.add(String.valueOf(map.get("questionId")));
                    }
                }
            }
        }
        return result;
    }

    private Map<String, Object> toDto(StudentTowerQuestionPack pack) {
        List<String> ids = readIds(pack.getQuestionIdsJson());
        List<Question> questions = ids.stream()
                .map(questionService::getById)
                .filter(Objects::nonNull)
                .toList();
        return toDto(pack, questions);
    }

    private Map<String, Object> toDto(StudentTowerQuestionPack pack, List<Question> questions) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("packId", pack.getPackId());
        result.put("runId", pack.getRunId());
        result.put("nodeId", pack.getNodeId());
        result.put("mode", pack.getMode());
        result.put("source", pack.getSource());
        result.put("aiReason", pack.getAiReason());
        result.put("strategy", readJson(pack.getStrategyJson()));
        result.put("questions", questions.stream().map(this::questionDto).toList());
        return result;
    }

    private Map<String, Object> questionDto(Question q) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("questionId", q.getQuestionId());
        item.put("courseCode", q.getCourseCode());
        item.put("lessonNo", q.getLessonNo());
        item.put("type", q.getType());
        item.put("stem", q.getStem());
        item.put("options", q.getOptions());
        item.put("answer", q.getAnswer());
        item.put("difficulty", q.getDifficulty());
        item.put("knowledgePointId", q.getKnowledgePointId());
        item.put("score", q.getScore());
        return item;
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

    private int targetCount(String mode, String roomType) {
        if ("diagnosis".equals(mode)) return 3;
        if ("boss".equals(mode) || "boss".equals(roomType)) return 8;
        if ("elite".equals(mode) || "elite".equals(roomType)) return 6;
        return 5;
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) return "battle";
        String normalized = mode.trim().toLowerCase();
        return switch (normalized) {
            case "diagnosis", "battle", "elite", "boss" -> normalized;
            default -> "battle";
        };
    }

    private List<String> readIds(String json) {
        Object parsed = readJson(json);
        if (!(parsed instanceof List<?> list)) return List.of();
        return list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
    }

    private Object readJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Object>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
*** Add File: Practical-training-master/backend/src/main/java/com/neu/CoursePlatform/service/impl/AbilityRadarServiceImpl.java
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
                if (topChange.isBlank() || Math.abs(delta) > Math.abs(afterSeries.isEmpty() ? 0 : delta)) {
                    topChange = ability.getName();
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
*** End Patch

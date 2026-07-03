package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.common.SharedIds;
import com.neu.CoursePlatform.entity.AbilityKnowledgePoint;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.entity.StudentTowerAttempt;
import com.neu.CoursePlatform.entity.StudentTowerNode;
import com.neu.CoursePlatform.entity.StudentTowerQuestionPack;
import com.neu.CoursePlatform.entity.StudentTowerRun;
import com.neu.CoursePlatform.mapper.AbilityKnowledgePointMapper;
import com.neu.CoursePlatform.mapper.StudentTowerAttemptMapper;
import com.neu.CoursePlatform.mapper.StudentTowerNodeMapper;
import com.neu.CoursePlatform.mapper.StudentTowerQuestionPackMapper;
import com.neu.CoursePlatform.mapper.StudentTowerRunMapper;
import com.neu.CoursePlatform.service.QuestionService;
import com.neu.CoursePlatform.service.TowerQuestionPackService;
import com.neu.CoursePlatform.service.demo.DemoTowerDataService;
import org.springframework.dao.DuplicateKeyException;
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
    private static final int STRATEGY_VERSION = 2;

    private final StudentTowerQuestionPackMapper packMapper;
    private final StudentTowerRunMapper runMapper;
    private final StudentTowerNodeMapper nodeMapper;
    private final StudentTowerAttemptMapper attemptMapper;
    private final AbilityKnowledgePointMapper abilityKnowledgePointMapper;
    private final QuestionService questionService;
    private final DemoTowerDataService demoTowerDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TowerQuestionPackServiceImpl(StudentTowerQuestionPackMapper packMapper,
                                        StudentTowerRunMapper runMapper,
                                        StudentTowerNodeMapper nodeMapper,
                                        StudentTowerAttemptMapper attemptMapper,
                                        AbilityKnowledgePointMapper abilityKnowledgePointMapper,
                                        QuestionService questionService,
                                        DemoTowerDataService demoTowerDataService) {
        this.packMapper = packMapper;
        this.runMapper = runMapper;
        this.nodeMapper = nodeMapper;
        this.attemptMapper = attemptMapper;
        this.abilityKnowledgePointMapper = abilityKnowledgePointMapper;
        this.questionService = questionService;
        this.demoTowerDataService = demoTowerDataService;
    }

    @Override
    public Map<String, Object> getOrCreateQuestionPack(String studentNo, String runId, String nodeId, String mode) {
        String normalizedMode = normalizeMode(mode);
        StudentTowerRun run = requireRun(studentNo, runId);
        StudentTowerNode node = requireNode(runId, nodeId);
        if (demoTowerDataService.isDemoSecondLevel(run, node)) {
            return demoTowerDataService.secondLevelQuestionPack(run, node, normalizedMode);
        }
        int targetCount = targetCount(normalizedMode, node.getRoomType());
        StudentTowerQuestionPack existing = packMapper.selectOne(new LambdaQueryWrapper<StudentTowerQuestionPack>()
                .eq(StudentTowerQuestionPack::getRunId, runId)
                .eq(StudentTowerQuestionPack::getNodeId, nodeId)
                .eq(StudentTowerQuestionPack::getMode, normalizedMode)
                .last("limit 1"));
        if (existing != null && isReusable(existing, targetCount)) return toDto(existing);
        return regenerateQuestionPack(studentNo, runId, nodeId, normalizedMode);
    }

    @Override
    @Transactional
    public Map<String, Object> regenerateQuestionPack(String studentNo, String runId, String nodeId, String mode) {
        String normalizedMode = normalizeMode(mode);
        StudentTowerRun run = requireRun(studentNo, runId);
        StudentTowerNode node = requireNode(runId, nodeId);
        if (demoTowerDataService.isDemoSecondLevel(run, node)) {
            return demoTowerDataService.secondLevelQuestionPack(run, node, normalizedMode);
        }
        int targetCount = targetCount(normalizedMode, node.getRoomType());
        Set<String> recentUsed = recentUsedQuestionIds(studentNo, run.getCourseCode());
        Set<String> runUsed = runUsedQuestionIds(runId, nodeId);

        Map<String, Question> candidateMap = layeredCandidates(run, node);
        boolean expanded = candidateMap.values().stream()
                .anyMatch(question -> !Objects.equals(question.getKnowledgePointId(), node.getKnowledgePointId()));
        List<Question> candidates = new ArrayList<>(candidateMap.values());

        List<Question> selected = selectQuestions(candidates, node, normalizedMode, recentUsed, runUsed, targetCount);
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
                "strategyVersion", STRATEGY_VERSION,
                "poolPolicy", "kp-ability-course",
                "excludeRunUsed", true,
                "targetCount", targetCount,
                "candidateCount", candidates.size(),
                "expandedToCourse", expanded,
                "knowledgePointId", safe(node.getKnowledgePointId()),
                "difficulty", node.getDifficulty() == null ? 1 : node.getDifficulty(),
                "recentUsedFiltered", recentUsed.size(),
                "runUsedExcluded", runUsed.size()
        )));
        pack.setAiReason("根据当前节点知识点、节点难度、近期做题记录和题型多样性从数据库题库中推荐。");
        pack.setCreatedAt(LocalDateTime.now());
        pack.setUpdatedAt(LocalDateTime.now());
        try {
            packMapper.insert(pack);
        } catch (DuplicateKeyException e) {
            StudentTowerQuestionPack existing = packMapper.selectOne(new LambdaQueryWrapper<StudentTowerQuestionPack>()
                    .eq(StudentTowerQuestionPack::getRunId, runId)
                    .eq(StudentTowerQuestionPack::getNodeId, nodeId)
                    .eq(StudentTowerQuestionPack::getMode, normalizedMode)
                    .last("limit 1"));
            if (existing != null) return toDto(existing);
            throw e;
        }
        return toDto(pack, selected);
    }

    private List<Question> selectQuestions(List<Question> candidates, StudentTowerNode node, String mode,
                                           Set<String> recentUsed, Set<String> runUsed, int targetCount) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        int targetDifficulty = Math.max(1, Math.min(5, node.getDifficulty() == null ? 1 : node.getDifficulty()));
        String kpId = safe(node.getKnowledgePointId());
        int salt = Math.abs(Objects.hash(node.getNodeId(), mode));
        List<Question> filtered = candidates.stream()
                .filter(q -> q.getQuestionId() != null)
                .filter(q -> !runUsed.contains(q.getQuestionId()))
                .toList();
        if (filtered.size() < targetCount) filtered = candidates;

        List<Question> sorted = filtered.stream()
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

    private Map<String, Question> layeredCandidates(StudentTowerRun run, StudentTowerNode node) {
        Map<String, Question> result = new LinkedHashMap<>();
        addQuestions(result, questionService.filterQuestions(run.getCourseCode(), null, node.getKnowledgePointId(), null, null, null));

        List<String> sameAbilityKps = sameAbilityKnowledgePoints(node.getKnowledgePointId());
        for (String kpId : sameAbilityKps) {
            if (!Objects.equals(kpId, node.getKnowledgePointId())) {
                addQuestions(result, questionService.filterQuestions(run.getCourseCode(), null, kpId, null, null, null));
            }
        }

        addQuestions(result, questionService.listByCourseCode(run.getCourseCode()));
        return result;
    }

    private void addQuestions(Map<String, Question> target, List<Question> questions) {
        if (questions == null) return;
        for (Question question : questions) {
            if (question.getQuestionId() != null) target.putIfAbsent(question.getQuestionId(), question);
        }
    }

    private List<String> sameAbilityKnowledgePoints(String knowledgePointId) {
        if (knowledgePointId == null || knowledgePointId.isBlank()) return List.of();
        List<AbilityKnowledgePoint> mappings = abilityKnowledgePointMapper.selectList(new LambdaQueryWrapper<AbilityKnowledgePoint>()
                .eq(AbilityKnowledgePoint::getKnowledgePointId, knowledgePointId));
        if (mappings.isEmpty()) return List.of();
        List<String> abilityIds = mappings.stream()
                .map(AbilityKnowledgePoint::getAbilityPointId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (abilityIds.isEmpty()) return List.of();
        return abilityKnowledgePointMapper.selectList(new LambdaQueryWrapper<AbilityKnowledgePoint>()
                        .in(AbilityKnowledgePoint::getAbilityPointId, abilityIds))
                .stream()
                .map(AbilityKnowledgePoint::getKnowledgePointId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
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

    private Set<String> runUsedQuestionIds(String runId, String exceptNodeId) {
        Set<String> result = new HashSet<>();
        List<StudentTowerQuestionPack> packs = packMapper.selectList(new LambdaQueryWrapper<StudentTowerQuestionPack>()
                .eq(StudentTowerQuestionPack::getRunId, runId)
                .ne(StudentTowerQuestionPack::getNodeId, exceptNodeId));
        for (StudentTowerQuestionPack pack : packs) {
            result.addAll(readIds(pack.getQuestionIdsJson()));
        }

        List<StudentTowerAttempt> attempts = attemptMapper.selectList(new LambdaQueryWrapper<StudentTowerAttempt>()
                .eq(StudentTowerAttempt::getRunId, runId)
                .ne(StudentTowerAttempt::getNodeId, exceptNodeId));
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

    private boolean isReusable(StudentTowerQuestionPack pack, int targetCount) {
        Object strategy = readJson(pack.getStrategyJson());
        if (!(strategy instanceof Map<?, ?> map)) return false;
        Object version = map.get("strategyVersion");
        int parsedVersion = version instanceof Number number ? number.intValue() : 0;
        if (parsedVersion < STRATEGY_VERSION) return false;
        return readIds(pack.getQuestionIdsJson()).size() >= targetCount;
    }

    private Map<String, Object> toDto(StudentTowerQuestionPack pack) {
        List<String> ids = readIds(pack.getQuestionIdsJson());
        List<Question> loadedQuestions = ids.isEmpty() ? List.of() : questionService.listByIds(ids);
        if (loadedQuestions == null) {
            loadedQuestions = ids.stream()
                    .map(questionService::getById)
                    .filter(Objects::nonNull)
                    .toList();
        }
        Map<String, Question> questionIndex = loadedQuestions.stream()
                .collect(java.util.stream.Collectors.toMap(Question::getQuestionId, question -> question, (a, b) -> a));
        List<Question> questions = ids.stream()
                .map(questionIndex::get)
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

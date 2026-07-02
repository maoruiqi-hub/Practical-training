package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.SubmissionAnswer;
import com.neu.CoursePlatform.service.AnalysisService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.SubmissionAnswerService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    private final SubmissionAnswerService answerService;
    private final KnowledgePointService knowledgePointService;
    private final LearningTaskService taskService;

    public AnalysisServiceImpl(SubmissionAnswerService answerService, KnowledgePointService knowledgePointService,
                               LearningTaskService taskService) {
        this.answerService = answerService;
        this.knowledgePointService = knowledgePointService;
        this.taskService = taskService;
    }

    @Override
    public Map<String, Object> buildStudentWrongStats(String studentNo, String taskNo, String knowledgePointId, String type) {
        return buildWrongStats(answerService.listByStudentNo(studentNo, taskNo, knowledgePointId, type));
    }

    @Override
    public Map<String, Object> buildTaskWrongStats(String taskNo) {
        return buildWrongStats(answerService.listByTaskNo(taskNo));
    }

    @Override
    public Map<String, Object> buildCourseWrongStats(String courseCode) {
        List<SubmissionAnswer> answers = new ArrayList<>();
        for (LearningTask task : taskService.listByCourseCode(courseCode)) {
            if (taskService.isQuizTask(task)) {
                answers.addAll(answerService.listByTaskNo(task.getTaskNo()));
            }
        }
        return buildWrongStats(answers);
    }

    private Map<String, Object> buildWrongStats(List<SubmissionAnswer> answers) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalAnswers", answers.size());
        result.put("wrongAnswers", answers.stream().filter(this::isWrong).count());
        result.put("byQuestion", aggregate(answers, SubmissionAnswer::getQuestionId));
        result.put("byKnowledgePoint", aggregateKnowledgePoint(answers));
        result.put("byType", aggregate(answers, a -> emptyAsUnknown(a.getQuestionType())));
        result.put("wrongList", buildWrongList(answers));
        result.put("mastery", buildMastery(answers));
        result.put("recommendations", buildRecommendations(answers));
        return result;
    }

    private List<Map<String, Object>> buildWrongList(List<SubmissionAnswer> answers) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (SubmissionAnswer answer : answers) {
            if (!isWrong(answer)) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("submissionId", answer.getSubmissionId());
            item.put("taskNo", answer.getTaskNo());
            item.put("questionId", answer.getQuestionId());
            item.put("stem", answer.getQuestionStem());
            item.put("questionType", answer.getQuestionType());
            item.put("knowledgePointId", answer.getKnowledgePointId());
            item.put("knowledgePointName", knowledgeName(answer.getKnowledgePointId()));
            item.put("studentAnswer", answer.getStudentAnswer());
            item.put("correctAnswer", answer.getCorrectAnswer());
            item.put("score", answer.getScore());
            item.put("maxScore", answer.getMaxScore());
            item.put("createTime", answer.getCreateTime());
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> aggregateKnowledgePoint(List<SubmissionAnswer> answers) {
        Map<String, List<SubmissionAnswer>> groups = new LinkedHashMap<>();
        for (SubmissionAnswer answer : answers) {
            groups.computeIfAbsent(emptyAsUnknown(answer.getKnowledgePointId()), k -> new ArrayList<>()).add(answer);
        }
        List<Map<String, Object>> stats = new ArrayList<>();
        groups.forEach((key, list) -> {
            long wrong = list.stream().filter(this::isWrong).count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", knowledgeName(key));
            item.put("knowledgePointId", key);
            item.put("total", list.size());
            item.put("wrong", wrong);
            item.put("wrongRate", list.isEmpty() ? 0 : Math.round(wrong * 1000.0 / list.size()) / 10.0);
            stats.add(item);
        });
        stats.sort((a, b) -> Long.compare(((Number) b.get("wrong")).longValue(), ((Number) a.get("wrong")).longValue()));
        return stats;
    }

    private List<Map<String, Object>> buildMastery(List<SubmissionAnswer> answers) {
        List<Map<String, Object>> mastery = new ArrayList<>();
        for (Map<String, Object> stat : aggregateKnowledgePoint(answers)) {
            long total = ((Number) stat.get("total")).longValue();
            long wrong = ((Number) stat.get("wrong")).longValue();
            double masteryRate = total == 0 ? 0 : Math.round((total - wrong) * 1000.0 / total) / 10.0;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("knowledgePointId", stat.get("knowledgePointId"));
            item.put("knowledgePointName", stat.get("key"));
            item.put("total", total);
            item.put("wrong", wrong);
            item.put("masteryRate", masteryRate);
            item.put("level", masteryLevel(masteryRate, total));
            mastery.add(item);
        }
        mastery.sort((a, b) -> Double.compare(((Number) a.get("masteryRate")).doubleValue(), ((Number) b.get("masteryRate")).doubleValue()));
        return mastery;
    }

    private List<Map<String, Object>> buildRecommendations(List<SubmissionAnswer> answers) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        for (Map<String, Object> item : buildMastery(answers)) {
            long total = ((Number) item.get("total")).longValue();
            long wrong = ((Number) item.get("wrong")).longValue();
            double masteryRate = ((Number) item.get("masteryRate")).doubleValue();
            if (total < 2 || wrong == 0 || masteryRate >= 80) continue;
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("knowledgePointId", item.get("knowledgePointId"));
            rec.put("knowledgePointName", item.get("knowledgePointName"));
            rec.put("priority", masteryRate < 60 ? "high" : "medium");
            rec.put("reason", "该知识点错误率较高，建议安排专项练习或回看对应课时。");
            recommendations.add(rec);
        }
        return recommendations;
    }

    private String masteryLevel(double masteryRate, long total) {
        if (total == 0) return "暂无数据";
        if (masteryRate >= 85) return "掌握较好";
        if (masteryRate >= 70) return "基本掌握";
        if (masteryRate >= 50) return "需要巩固";
        return "薄弱";
    }

    private List<Map<String, Object>> aggregate(List<SubmissionAnswer> answers,
                                                java.util.function.Function<SubmissionAnswer, String> classifier) {
        Map<String, List<SubmissionAnswer>> groups = new LinkedHashMap<>();
        for (SubmissionAnswer answer : answers) {
            groups.computeIfAbsent(classifier.apply(answer), k -> new ArrayList<>()).add(answer);
        }
        List<Map<String, Object>> stats = new ArrayList<>();
        groups.forEach((key, list) -> {
            long wrong = list.stream().filter(this::isWrong).count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", key);
            item.put("total", list.size());
            item.put("wrong", wrong);
            item.put("wrongRate", list.isEmpty() ? 0 : Math.round(wrong * 1000.0 / list.size()) / 10.0);
            stats.add(item);
        });
        stats.sort((a, b) -> Long.compare(((Number) b.get("wrong")).longValue(), ((Number) a.get("wrong")).longValue()));
        return stats;
    }

    private boolean isWrong(SubmissionAnswer answer) {
        return Boolean.TRUE.equals(answer.getAutoGradable()) && Boolean.FALSE.equals(answer.getCorrect());
    }

    private String emptyAsUnknown(String value) {
        return value == null || value.isBlank() ? "未分类" : value;
    }

    private String knowledgeName(String knowledgePointId) {
        if (knowledgePointId == null || knowledgePointId.isBlank() || "未分类".equals(knowledgePointId)) return "未分类";
        var point = knowledgePointService.getById(knowledgePointId);
        return point != null ? point.getName() : "未分类";
    }
}

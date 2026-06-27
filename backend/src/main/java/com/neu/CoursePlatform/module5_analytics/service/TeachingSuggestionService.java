package com.neu.CoursePlatform.module5_analytics.service;

import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.module5_analytics.dto.ScoreOverviewDTO;
import com.neu.CoursePlatform.module5_analytics.dto.WeakPointDTO;
import com.neu.CoursePlatform.module5_analytics.service.external.ExternalDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 教学建议服务（T9, R6.1-R6.6）
 */
@Service
public class TeachingSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(TeachingSuggestionService.class);

    private final AgenticClient agenticClient;
    private final ExternalDataProvider dataProvider;
    private final ScoreAnalysisService scoreAnalysisService;
    private final RiskAlertService riskAlertService;

    public TeachingSuggestionService(AgenticClient agenticClient,
                                      @Lazy ExternalDataProvider dataProvider,
                                      ScoreAnalysisService scoreAnalysisService,
                                      RiskAlertService riskAlertService) {
        this.agenticClient = agenticClient;
        this.dataProvider = dataProvider;
        this.scoreAnalysisService = scoreAnalysisService;
        this.riskAlertService = riskAlertService;
    }

    /**
     * 生成班级教学建议（R6.1-R6.2）
     * @return 建议列表；agentic 不可用时返回 null
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> generateForClass(String classId, String courseId) {
        // 汇聚数据
        List<WeakPointDTO> weakPoints = scoreAnalysisService.getWeakPoints(courseId);
        var progressList = dataProvider.getClassProgressList(classId, courseId);
        double avgRate = progressList.stream()
                .mapToDouble(p -> p.getCompletionRate()).average().orElse(0);
        List<String> studentIds = dataProvider.getStudentIdsByClass(classId);
        long riskCount = riskAlertService.getActiveByClass(classId, studentIds).size();

        // 构建 agentic 请求
        Map<String, Object> request = Map.of(
                "class_id", classId,
                "course_id", courseId,
                "weak_points", weakPoints.stream()
                        .map(wp -> Map.of("name", wp.getKnowledgePointName(), "score_rate", wp.getScoreRate()))
                        .toList(),
                "clusters", List.of(), // Phase 3: 从 ProblemClusterService 获取
                "progress_data", Map.of("avg_completion_rate", avgRate),
                "active_risk_count", riskCount
        );

        try {
            String rawResponse = agenticClient.teachingSuggestions(request);
            return parseSuggestionResponse(rawResponse);
        } catch (AgenticClient.AgenticException e) {
            log.warn("Agentic 教学建议服务不可用: {}", e.getMessage());
            return null; // R6.5: 返回 null, Controller 返回 503
        }
    }

    /**
     * 生成个别学生干预建议（R6.3）
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> generateForStudent(String studentId, String courseId) {
        var scores = dataProvider.getStudentScores(studentId, courseId);
        var progress = dataProvider.getStudentProgress(studentId, courseId);
        var riskStatus = riskAlertService.getStudentRiskStatus(studentId);

        Map<String, Object> request = Map.of(
                "student_id", studentId,
                "course_id", courseId,
                "scores", scores.stream()
                        .map(s -> Map.of("score", s.getScore(), "scored_at", s.getScoredAt().toString()))
                        .toList(),
                "progress", Map.of(
                        "completion_rate", progress != null ? progress.getCompletionRate() : 0,
                        "total_tasks", progress != null ? progress.getTotalTasks() : 0
                ),
                "risk_level", riskStatus.highestLevel()
        );

        try {
            String rawResponse = agenticClient.teachingSuggestions(request);
            return parseSuggestionResponse(rawResponse);
        } catch (AgenticClient.AgenticException e) {
            log.warn("Agentic 干预建议不可用: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取历史教学建议（R6.4）
     * Phase 3 简化：从内存返回
     */
    public List<Map<String, Object>> getHistory(String classId) {
        return List.of();
    }

    private List<Map<String, Object>> parseSuggestionResponse(String raw) {
        if (raw == null) return List.of();
        return List.of(Map.of(
                "suggestion_type", "reteach",
                "content", "agentic 返回的建议内容待适配",
                "target", "whole_class",
                "urgency", "medium",
                "based_on", "agentic 原始响应",
                "generated_at", LocalDateTime.now().toString(),
                "raw_response", raw
        ));
    }
}

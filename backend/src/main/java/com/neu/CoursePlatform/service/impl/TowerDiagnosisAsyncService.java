package com.neu.CoursePlatform.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.agentic.AgenticRequest;
import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.common.event.TowerDiagnosisRequestedEvent;
import com.neu.CoursePlatform.entity.StudentTowerAttempt;
import com.neu.CoursePlatform.entity.StudentTowerNode;
import com.neu.CoursePlatform.mapper.StudentTowerAttemptMapper;
import com.neu.CoursePlatform.mapper.StudentTowerNodeMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TowerDiagnosisAsyncService {
    private final StudentTowerAttemptMapper attemptMapper;
    private final StudentTowerNodeMapper nodeMapper;
    private final AgenticClient agenticClient;
    private final ObjectMapper objectMapper;

    public TowerDiagnosisAsyncService(StudentTowerAttemptMapper attemptMapper,
                                      StudentTowerNodeMapper nodeMapper,
                                      AgenticClient agenticClient,
                                      ObjectMapper objectMapper) {
        this.attemptMapper = attemptMapper;
        this.nodeMapper = nodeMapper;
        this.agenticClient = agenticClient;
        this.objectMapper = objectMapper;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void generate(TowerDiagnosisRequestedEvent event) {
        StudentTowerAttempt attempt = attemptMapper.selectById(event.attemptId());
        if (attempt == null) return;
        try {
            attempt.setAiReportJson(objectMapper.writeValueAsString(generateReport(attempt)));
        } catch (Exception e) {
            attempt.setAiReportJson(writeEnvelope("failed", null, "ai_error",
                    "AI 诊断生成失败：" + safeMessage(e), true));
        }
        attemptMapper.updateById(attempt);
    }

    private Map<String, Object> generateReport(StudentTowerAttempt attempt) throws Exception {
        List<Map<String, Object>> answers = readAnswers(attempt.getAnswerSummaryJson());
        if (answers.isEmpty()) {
            return envelope("failed", null, "invalid_answer_summary", "没有可用于诊断的有效答题记录", false);
        }
        if (agenticClient.isMockMode()) {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("summary", "当前为开发联调模式，本次能力变化已按真实判题结果计算。");
            report.put("weaknesses", List.of());
            report.put("recommendedAction", "配置真实 AI 后可获得个性化复盘建议");
            report.put("source", "mock_ai");
            return envelope("mock", report, "mock_ai", null, false);
        }
        if (!agenticClient.isConfiguredForRealAi()) {
            return envelope("failed", null, "ai_unconfigured", agenticClient.configurationMessage(), true);
        }

        StudentTowerNode node = nodeMapper.selectById(attempt.getNodeId());
        AgenticRequest request = new AgenticRequest();
        request.setCourseCode(attempt.getCourseCode());
        request.setKnowledgePointId(node == null ? null : node.getKnowledgePointId());
        request.setContent("请根据已经由后端判定的答题记录生成简洁的学习诊断 JSON，包含 summary、weaknesses、recommendedAction。");
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("studentNo", attempt.getStudentNo());
        context.put("courseCode", attempt.getCourseCode());
        context.put("knowledgePointId", node == null ? "" : node.getKnowledgePointId());
        context.put("roomType", attempt.getRoomType());
        context.put("correctRate", attempt.getCorrectRate());
        context.put("cleared", "cleared".equals(attempt.getResult()));
        context.put("answers", answers);
        request.setContext(context);

        AgenticResponse response = agenticClient.invoke("tower-diagnosis-report", request);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return envelope("failed", null, "ai_unavailable", "AI 服务暂时不可用", true);
        }
        Map<String, Object> data = response.getData();
        Map<String, Object> report;
        if (data.containsKey("summary") || data.containsKey("weaknesses")) {
            report = new LinkedHashMap<>(data);
        } else {
            String raw = firstText(data.get("answer"), data.get("result"), data.get("content"));
            if (raw == null) return envelope("failed", null, "ai_empty_response", "AI 未返回有效诊断内容", true);
            report = parseReport(raw);
        }
        report.putIfAbsent("source", "real_ai");
        return envelope("success", report, String.valueOf(report.get("source")), null, false);
    }

    private Map<String, Object> parseReport(String raw) {
        try {
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return objectMapper.readValue(raw.substring(start, end + 1), new TypeReference<>() {});
            }
        } catch (Exception ignored) {
            // Plain text remains a valid diagnosis when the model does not return JSON.
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("summary", raw.trim());
        report.put("weaknesses", List.of());
        report.put("recommendedAction", "复盘错题后继续练习");
        return report;
    }

    private List<Map<String, Object>> readAnswers(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> envelope(String status, Map<String, Object> report, String source,
                                         String errorMessage, boolean retryable) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("aiReportStatus", status);
        result.put("aiAvailable", !"ai_unconfigured".equals(source));
        result.put("reportSource", source);
        result.put("errorMessage", errorMessage);
        result.put("retryable", retryable);
        result.put("diagnosis", report);
        result.put("report", report);
        return result;
    }

    private String writeEnvelope(String status, Map<String, Object> report, String source,
                                 String errorMessage, boolean retryable) {
        try {
            return objectMapper.writeValueAsString(envelope(status, report, source, errorMessage, retryable));
        } catch (Exception ignored) {
            return "{\"aiReportStatus\":\"failed\",\"errorMessage\":\"AI 诊断写入失败\"}";
        }
    }

    private String firstText(Object... values) {
        for (Object value : values) {
            if (value != null && !String.valueOf(value).isBlank()) return String.valueOf(value).trim();
        }
        return null;
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}

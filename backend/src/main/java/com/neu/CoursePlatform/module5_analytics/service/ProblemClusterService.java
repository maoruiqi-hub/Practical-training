package com.neu.CoursePlatform.module5_analytics.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.module5_analytics.dto.WeakPointDTO;
import com.neu.CoursePlatform.module5_analytics.service.external.ExternalDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 共性问题聚类服务（T8, R5.1-R5.6）
 */
@Service
public class ProblemClusterService {

    private static final Logger log = LoggerFactory.getLogger(ProblemClusterService.class);

    private final AgenticClient agenticClient;
    private final ExternalDataProvider dataProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProblemClusterService(AgenticClient agenticClient,
                                  ExternalDataProvider dataProvider) {
        this.agenticClient = agenticClient;
        this.dataProvider = dataProvider;
    }

    /**
     * 执行共性问题聚类分析（R5.1-R5.3）
     * @return 聚类结果列表；agentic 不可用时返回 null
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> cluster(String classId, String courseId) {
        // 1. 汇聚数据
        var mistakeStats = dataProvider.getClassMistakeStats(courseId);
        List<String> studentIds = dataProvider.getStudentIdsByClass(classId);
        List<Map<String, Object>> mistakes = new ArrayList<>();
        for (String sid : studentIds) {
            // 收集每个学生的错题信息（简化：用统计数据代替）
            for (var stat : mistakeStats) {
                mistakes.add(Map.of(
                        "student_id", sid,
                        "knowledge_point_id", stat.getKnowledgePointId(),
                        "knowledge_point_name", stat.getKnowledgePointName(),
                        "mistake_rate", stat.getMistakeRate()
                ));
            }
        }

        // 2. 构建 agentic 请求
        Map<String, Object> request = Map.of(
                "class_id", classId,
                "course_id", courseId,
                "mistakes", mistakes,
                "feedbacks", List.of(),  // Phase 3: 从模块2获取反馈文本
                "questions", List.of()
        );

        // 3. 调用 agentic
        try {
            String rawResponse = agenticClient.clusterProblems(request);
            // 解析响应（简化：假设返回 JSON 数组）
            return parseClusterResponse(rawResponse);
        } catch (AgenticClient.AgenticException e) {
            log.warn("Agentic 聚类服务不可用: {}", e.getMessage());
            return null; // R5.5: 返回 null 表示不可用，Controller 返回 503
        }
    }

    /**
     * 获取最近一次聚类结果（R5.4, R5.6）
     * Phase 3 简化：无持久化存储，返回空
     */
    public List<Map<String, Object>> getLatestCluster(String classId) {
        // Phase 4: 从 AnalyticsReport 表读取历史聚类结果
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseClusterResponse(String raw) {
        try {
            String json = extractJson(raw);
            if (json != null && json.startsWith("[")) {
                List<Map<String, Object>> items = objectMapper.readValue(json, new TypeReference<>() {});
                String generatedAt = LocalDateTime.now().toString();
                return items.stream().map(item -> {
                    Map<String, Object> normalized = new LinkedHashMap<>(item);
                    normalized.putIfAbsent("generated_at", generatedAt);
                    return normalized;
                }).toList();
            }
            if (json != null && json.startsWith("{")) {
                JsonNode node = objectMapper.readTree(json);
                JsonNode items = node.path("clusters");
                if (!items.isArray()) items = node.path("items");
                if (!items.isArray()) items = node.path("data");
                if (items.isArray()) {
                    List<Map<String, Object>> result = objectMapper.convertValue(items, new TypeReference<>() {});
                    String generatedAt = LocalDateTime.now().toString();
                    return result.stream().map(item -> {
                        Map<String, Object> normalized = new LinkedHashMap<>(item);
                        normalized.putIfAbsent("generated_at", generatedAt);
                        return normalized;
                    }).toList();
                }
            }
        } catch (Exception e) {
            log.warn("解析聚类响应失败: {}", e.getMessage());
        }
        return List.of(Map.of(
                "topic", "聚类结果",
                "note", "agentic 返回格式待适配",
                "raw_response", raw
        ));
    }

    private String extractJson(String raw) {
        if (raw == null) return null;
        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }
        int arrayStart = text.indexOf('[');
        int objectStart = text.indexOf('{');
        int start;
        if (arrayStart >= 0 && objectStart >= 0) start = Math.min(arrayStart, objectStart);
        else start = Math.max(arrayStart, objectStart);
        if (start < 0) return text;
        int end = text.charAt(start) == '[' ? text.lastIndexOf(']') : text.lastIndexOf('}');
        if (end < start) return text.substring(start);
        return text.substring(start, end + 1).trim();
    }
}

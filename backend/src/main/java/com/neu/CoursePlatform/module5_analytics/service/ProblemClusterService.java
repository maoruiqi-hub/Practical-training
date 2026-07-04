package com.neu.CoursePlatform.module5_analytics.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.module5_analytics.dto.external.MistakeStatsDTO;
import com.neu.CoursePlatform.module5_analytics.service.external.ExternalDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 共性问题聚类服务（T8, R5.1-R5.6）
 */
@Service
public class ProblemClusterService {

    private static final Logger log = LoggerFactory.getLogger(ProblemClusterService.class);
    private static final long AI_TIMEOUT_SECONDS = 15;

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
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String rawResponse = agenticClient.clusterProblems(request);
                    return parseClusterResponse(rawResponse);
                } catch (AgenticClient.AgenticException e) {
                    throw new RuntimeException(e);
                }
            }).get(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Agentic cluster timed out after {} seconds, using fallback result", AI_TIMEOUT_SECONDS);
            return buildFallbackClusters(mistakeStats, studentIds.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Agentic cluster interrupted, using fallback result");
            return buildFallbackClusters(mistakeStats, studentIds.size());
        } catch (ExecutionException e) {
            log.warn("Agentic cluster failed, using fallback result: {}", e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
            return buildFallbackClusters(mistakeStats, studentIds.size());
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

    private List<Map<String, Object>> buildFallbackClusters(List<MistakeStatsDTO> mistakeStats, int studentCount) {
        String generatedAt = LocalDateTime.now().toString();
        if (mistakeStats == null || mistakeStats.isEmpty()) {
            return List.of(new LinkedHashMap<>(Map.of(
                    "topic", "暂无明显共性问题",
                    "student_count", 0,
                    "description", "当前课程暂无足够错题数据，建议先积累测验作答后再分析。",
                    "suggested_action", "补充一次随堂测验或章节练习，获取更稳定的班级薄弱点。",
                    "generated_at", generatedAt,
                    "source", "fallback"
            )));
        }

        return mistakeStats.stream()
                .sorted(Comparator.comparingDouble(MistakeStatsDTO::getMistakeRate).reversed())
                .limit(5)
                .map(stat -> {
                    String kpName = Optional.ofNullable(stat.getKnowledgePointName())
                            .filter(name -> !name.isBlank())
                            .orElse("未命名知识点");
                    int affected = Math.max(stat.getMistakeCount(),
                            (int) Math.ceil(Math.max(studentCount, 1) * stat.getMistakeRate()));
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("topic", kpName + "共性错误");
                    item.put("student_count", affected);
                    item.put("knowledge_points", List.of(kpName));
                    item.put("description", String.format(Locale.ROOT,
                            "%s 错误率约 %.1f%%，共 %d 次作答中出现 %d 次错误。",
                            kpName, stat.getMistakeRate() * 100, stat.getTotalAttempts(), stat.getMistakeCount()));
                    item.put("suggested_action", "安排针对该知识点的例题讲解和短练习，课后跟踪订正情况。");
                    item.put("generated_at", generatedAt);
                    item.put("source", "fallback");
                    return item;
                })
                .toList();
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

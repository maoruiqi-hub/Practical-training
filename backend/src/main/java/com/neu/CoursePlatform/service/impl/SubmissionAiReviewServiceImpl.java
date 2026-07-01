package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.agentic.AgenticRequest;
import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.SubmissionAiReview;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.mapper.SubmissionAiReviewMapper;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.SubmissionAiReviewService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SubmissionAiReviewServiceImpl extends ServiceImpl<SubmissionAiReviewMapper, SubmissionAiReview> implements SubmissionAiReviewService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionAiReviewServiceImpl.class);

    private final TaskSubmissionService submissionService;
    private final LearningTaskService taskService;
    private final AgenticClient agenticClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SubmissionAiReviewServiceImpl(TaskSubmissionService submissionService, LearningTaskService taskService,
                                         AgenticClient agenticClient) {
        this.submissionService = submissionService;
        this.taskService = taskService;
        this.agenticClient = agenticClient;
    }

    @Override
    public SubmissionAiReview getLatestBySubmissionId(String submissionId) {
        return getOne(new QueryWrapper<SubmissionAiReview>()
                .eq("submission_id", submissionId)
                .orderByDesc("create_time")
                .last("LIMIT 1"));
    }

    @Override
    public SubmissionAiReview generateReview(String submissionId) {
        TaskSubmission submission = submissionService.getById(submissionId);
        if (submission == null) throw new IllegalArgumentException("提交记录不存在");
        LearningTask task = taskService.getById(submission.getTaskNo());

        String content = taskService.isQuizTask(task)
                ? buildQuizReviewContent(submission, task)
                : normalize(submission.getContent());
        boolean hasFile = submission.getFilePath() != null && !submission.getFilePath().isBlank();
        ReviewDraft draft = generateWithAgentic(content, hasFile, task);
        if (draft == null) draft = generateLocalDraft(content, hasFile, task);

        SubmissionAiReview review = new SubmissionAiReview();
        review.setSubmissionId(submission.getSubmissionId());
        review.setTaskNo(submission.getTaskNo());
        review.setStudentNo(submission.getStudentNo());
        review.setAiScore(draft.score());
        review.setDimensions(toJson(draft.dimensions()));
        review.setSummary(draft.summary());
        review.setSuggestions(toJson(draft.suggestions()));
        review.setRiskLevel(draft.riskLevel());
        review.setStatus("generated");
        review.setCreateTime(LocalDateTime.now());
        save(review);
        return review;
    }

    private ReviewDraft generateWithAgentic(String content, boolean hasFile, LearningTask task) {
        try {
            AgenticRequest request = new AgenticRequest();
            request.setCourseCode(task != null ? task.getCourseCode() : null);
            request.setContent(buildAgenticPrompt(content, hasFile, task));
            request.setContext(Map.of(
                    "taskNo", task != null ? task.getTaskNo() : "",
                    "taskType", task != null ? normalize(task.getTaskType()) : "",
                    "hasFile", hasFile
            ));
            AgenticResponse response = agenticClient.invoke("assessment", request);
            if (response == null || !response.isSuccess() || response.getData() == null || response.getData().isEmpty()) {
                return null;
            }
            String json = objectMapper.writeValueAsString(response.getData());
            return parseDraft(json);
        } catch (Exception e) {
            log.warn("Agentic assessment failed, fallback to local draft. submission task={}", task != null ? task.getTaskNo() : "", e);
            return null;
        }
    }

    private String buildAgenticPrompt(String content, boolean hasFile, LearningTask task) {
        return """
                请对学生提交进行辅助评价，评价结果仅供教师复核参考。
                如果材料中包含填空题、简答题或编程题，请结合题干、参考答案、学生答案和得分点给出评分建议；填空题允许合理同义表达，但不要替代教师最终判断。
                任务类型：%s
                任务要求：%s
                是否包含附件：%s
                学生文字提交：
                %s

                请返回如下 JSON：
                {
                  "score": 0-100整数,
                  "dimensions": {
                    "内容完整性": 0-100整数,
                    "知识点覆盖度": 0-100整数,
                    "逻辑结构": 0-100整数,
                    "表达规范": 0-100整数,
                    "任务要求符合度": 0-100整数
                  },
                  "summary": "一句评价摘要",
                  "suggestions": ["修改建议1", "修改建议2"],
                  "riskLevel": "low|medium|high"
                }
                """.formatted(
                task != null ? normalize(task.getTaskType()) : "",
                task != null ? normalize(task.getDescription()) : "",
                hasFile ? "是" : "否",
                content.isBlank() ? "（无文字提交）" : content
        );
    }

    private String buildQuizReviewContent(TaskSubmission submission, LearningTask task) {
        Map<String, Object> detail = submissionService.buildGradeDetail(submission.getSubmissionId());
        Object rawDetails = detail != null ? detail.get("details") : null;
        if (!(rawDetails instanceof List<?> details)) {
            throw new IllegalArgumentException("测验作答详情不存在");
        }

        List<String> questionBlocks = new ArrayList<>();
        int index = 1;
        for (Object item : details) {
            if (!(item instanceof Map<?, ?> rawMap)) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            rawMap.forEach((key, value) -> row.put(String.valueOf(key), value));
            String type = normalize(String.valueOf(row.getOrDefault("type", "")));
            if (!needsAiQuestionReview(type)) continue;
            questionBlocks.add("""
                    %d. 题型：%s
                    题干：%s
                    参考答案/评分要点：%s
                    学生答案：%s
                    题目满分：%s
                    系统已给分：%s
                    """.formatted(
                    index++,
                    type,
                    normalize(String.valueOf(row.getOrDefault("stem", ""))),
                    normalize(String.valueOf(row.getOrDefault("correctAnswer", ""))),
                    normalize(String.valueOf(row.getOrDefault("studentAnswer", ""))),
                    String.valueOf(row.getOrDefault("score", "")),
                    String.valueOf(row.getOrDefault("earnedScore", ""))
            ));
        }

        if (questionBlocks.isEmpty()) {
            throw new IllegalArgumentException("当前提交没有需要 AI 评价的填空题、简答题或编程题");
        }
        return """
                在线测验主观/半主观题复核材料
                测验任务：%s
                说明：请只评价下面列出的填空题、简答题或编程题，不需要评价单选/多选客观题。

                %s
                """.formatted(
                task != null ? normalize(task.getDescription()) : "",
                String.join("\n", questionBlocks)
        );
    }

    private boolean needsAiQuestionReview(String type) {
        return List.of("fill", "essay", "program").contains(type);
    }

    private ReviewDraft parseDraft(String json) throws Exception {
        String normalized = json.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```json\\s*", "").replaceFirst("^```\\s*", "").replaceFirst("\\s*```$", "");
        }
        JsonNode root = objectMapper.readTree(normalized);
        Map<String, Integer> dimensions = new LinkedHashMap<>();
        JsonNode dimNode = root.path("dimensions");
        dimensions.put("内容完整性", clamp(dimNode.path("内容完整性").asInt(0)));
        dimensions.put("知识点覆盖度", clamp(dimNode.path("知识点覆盖度").asInt(0)));
        dimensions.put("逻辑结构", clamp(dimNode.path("逻辑结构").asInt(0)));
        dimensions.put("表达规范", clamp(dimNode.path("表达规范").asInt(0)));
        dimensions.put("任务要求符合度", clamp(dimNode.path("任务要求符合度").asInt(0)));
        List<String> suggestions = new ArrayList<>();
        JsonNode suggestionNode = root.path("suggestions");
        if (suggestionNode.isArray()) {
            suggestionNode.forEach(item -> suggestions.add(item.asText()));
        }
        if (suggestions.isEmpty()) suggestions.add("建议教师结合提交内容进行最终复核。");
        int score = root.has("score")
                ? clamp(root.path("score").asInt())
                : Math.round((float) dimensions.values().stream().mapToInt(Integer::intValue).average().orElse(0));
        String riskLevel = normalize(root.path("riskLevel").asText(score >= 80 ? "low" : score >= 60 ? "medium" : "high"));
        if (!List.of("low", "medium", "high").contains(riskLevel)) riskLevel = score >= 80 ? "low" : score >= 60 ? "medium" : "high";
        String summary = normalize(root.path("summary").asText());
        if (summary.isBlank()) summary = buildSummary(score, "", false);
        return new ReviewDraft(score, dimensions, summary, suggestions, riskLevel);
    }

    private ReviewDraft generateLocalDraft(String content, boolean hasFile, LearningTask task) {
        Map<String, Integer> dimensions = buildDimensions(content, hasFile, task);
        int score = Math.round((float) dimensions.values().stream().mapToInt(Integer::intValue).average().orElse(0));
        List<String> suggestions = buildSuggestions(content, hasFile, dimensions, task);
        return new ReviewDraft(score, dimensions, buildSummary(score, content, hasFile), suggestions,
                score >= 80 ? "low" : score >= 60 ? "medium" : "high");
    }

    private Map<String, Integer> buildDimensions(String content, boolean hasFile, LearningTask task) {
        Map<String, Integer> dimensions = new LinkedHashMap<>();
        int lengthScore = content.length() >= 600 ? 90 : content.length() >= 300 ? 78 : content.length() >= 120 ? 65 : hasFile ? 60 : 45;
        int taskFit = task != null && containsAny(content, task.getDescription()) ? 82 : content.length() >= 120 || hasFile ? 68 : 50;
        int structure = containsAny(content, "一、", "1.", "首先", "其次", "最后", "总结", "结论") ? 82 : content.length() >= 300 ? 70 : 55;
        int expression = content.isBlank() && hasFile ? 62 : content.length() >= 120 ? 76 : 58;
        int coverage = containsAny(content, "知识点", "实验", "结果", "分析", "代码", "函数", "数据") ? 78 : 60;
        dimensions.put("内容完整性", lengthScore);
        dimensions.put("知识点覆盖度", coverage);
        dimensions.put("逻辑结构", structure);
        dimensions.put("表达规范", expression);
        dimensions.put("任务要求符合度", taskFit);
        return dimensions;
    }

    private List<String> buildSuggestions(String content, boolean hasFile, Map<String, Integer> dimensions, LearningTask task) {
        List<String> suggestions = new ArrayList<>();
        if (content.length() < 120 && !hasFile) suggestions.add("提交内容偏少，建议补充实现过程、关键步骤和结果说明。");
        if (content.length() < 120 && hasFile) suggestions.add("当前主要依赖附件，建议在文字说明中概括报告结构、核心结论和关键知识点。");
        if (dimensions.getOrDefault("逻辑结构", 0) < 70) suggestions.add("建议按背景、过程、结果、问题分析、总结的结构组织内容。");
        if (dimensions.getOrDefault("知识点覆盖度", 0) < 70) suggestions.add("建议明确写出涉及的知识点，并说明这些知识点如何用于完成任务。");
        if (task != null && !containsAny(content, task.getDescription())) suggestions.add("建议对照任务要求逐项说明完成情况，避免只描述笼统结果。");
        if (suggestions.isEmpty()) suggestions.add("整体完成度较好，可进一步补充反思、边界情况和改进方向。");
        return suggestions;
    }

    private String buildSummary(int score, String content, boolean hasFile) {
        if (score >= 80) return "提交内容较完整，结构和任务匹配度较好，可作为教师复核参考。";
        if (score >= 60) return hasFile ? "提交具备基础材料，但文字说明和结构化表达仍可加强。" : "提交具备基础内容，但完整性、结构和知识点说明仍需完善。";
        return content.isBlank() && hasFile ? "提交主要为附件，系统无法充分判断内容质量，建议教师重点查看附件。" : "提交内容较弱，建议补充任务过程、知识点说明和结果分析。";
    }

    private boolean containsAny(String content, String... values) {
        if (content == null) return false;
        for (String value : values) {
            if (value != null && !value.isBlank() && content.contains(value)) return true;
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private record ReviewDraft(int score, Map<String, Integer> dimensions, String summary,
                               List<String> suggestions, String riskLevel) {
    }
}

package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.agentic.AgenticRequest;
import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.entity.SubmissionAiReview;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.entity.TaskQuestion;
import com.neu.CoursePlatform.mapper.SubmissionAiReviewMapper;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.LearningEvidenceService;
import com.neu.CoursePlatform.service.SubmissionAiReviewService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import com.neu.CoursePlatform.service.QuestionService;
import com.neu.CoursePlatform.service.TaskQuestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SubmissionAiReviewServiceImpl extends ServiceImpl<SubmissionAiReviewMapper, SubmissionAiReview> implements SubmissionAiReviewService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionAiReviewServiceImpl.class);

    private final TaskSubmissionService submissionService;
    private final LearningTaskService taskService;
    private final AgenticClient agenticClient;
    private final QuestionService questionService;
    private final TaskQuestionService taskQuestionService;
    private final LearningEvidenceService learningEvidenceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SubmissionAiReviewServiceImpl(TaskSubmissionService submissionService, LearningTaskService taskService,
                                         AgenticClient agenticClient, QuestionService questionService,
                                         TaskQuestionService taskQuestionService,
                                         LearningEvidenceService learningEvidenceService) {
        this.submissionService = submissionService;
        this.taskService = taskService;
        this.agenticClient = agenticClient;
        this.questionService = questionService;
        this.taskQuestionService = taskQuestionService;
        this.learningEvidenceService = learningEvidenceService;
    }

    @Override
    public SubmissionAiReview getLatestBySubmissionId(String submissionId) {
        return getOne(new QueryWrapper<SubmissionAiReview>()
                .eq("submission_id", submissionId)
                .orderByDesc("create_time")
                .last("LIMIT 1"));
    }

    @Override
    @Async
    public void requestAutomaticReview(String submissionId) {
        try {
            generateReview(submissionId);
        } catch (Exception e) {
            log.warn("Automatic AI assessment failed; manual review remains available, submission={}", submissionId, e);
        }
    }

    @Override
    public SubmissionAiReview generateReview(String submissionId) {
        TaskSubmission submission = submissionService.getById(submissionId);
        if (submission == null) throw new IllegalArgumentException("提交记录不存在");
        SubmissionAiReview latest = getLatestBySubmissionId(submissionId);
        if (latest != null && latest.getCreateTime() != null
                && latest.getCreateTime().isAfter(LocalDateTime.now().minusMinutes(5))) {
            return latest;
        }
        LearningTask task = taskService.getById(submission.getTaskNo());

        String content = taskService.isQuizTask(task)
                ? buildQuizReviewContent(submission, task)
                : normalize(submission.getContent());
        boolean hasFile = submission.getFilePath() != null && !submission.getFilePath().isBlank();
        // 没有真实模型结果时不能用本地规则伪造评分；提交保留并转教师复核。
        ReviewDraft draft = generateWithAgentic(content, hasFile, task);

        SubmissionAiReview review = new SubmissionAiReview();
        review.setSubmissionId(submission.getSubmissionId());
        review.setTaskNo(submission.getTaskNo());
        review.setStudentNo(submission.getStudentNo());
        review.setAiScore(draft == null ? null : draft.score());
        review.setConfidence(draft == null ? null : draft.confidence());
        review.setBasis(draft == null ? null : draft.basis());
        review.setDimensions(toJson(draft == null ? Map.of() : draft.dimensions()));
        review.setSummary(draft == null ? "模型评价暂不可用，请教师复核原始提交。" : draft.summary());
        review.setSuggestions(toJson(draft == null
                ? List.of("请检查模型服务配置，或由教师直接复核。")
                : draft.suggestions()));
        review.setRiskLevel(draft == null ? "high" : draft.riskLevel());
        boolean accepted = draft != null && draft.confidence() >= 0.70D;
        review.setStatus(draft == null ? "pending_review" : accepted ? "accepted" : "needs_review");
        review.setCreateTime(LocalDateTime.now());
        save(review);
        if (accepted) applyAcceptedAssessment(submission, task, draft);
        return review;
    }

    private void applyAcceptedAssessment(TaskSubmission submission, LearningTask task, ReviewDraft draft) {
        if (task == null || draft == null || submission == null) return;
        int maxScore = task.getScore() == null ? 100 : Math.max(0, task.getScore());
        int finalScore = Math.max(0, Math.min(maxScore, Math.round(maxScore * draft.score() / 100F)));
        submission.setPreviousScore(submission.getScore());
        submission.setScore(finalScore);
        submission.setStatus("graded");
        submission.setFeedback("AI 自动评价已生成，教师可复核覆盖；" + (draft.summary() == null ? "" : draft.summary()));
        submissionService.updateById(submission);

        List<TaskQuestion> bindings = taskQuestionService.listByTaskNo(task.getTaskNo());
        // 总分评价不能复制成多道题的证据；多题任务等待后续逐题评价或教师复核。
        if (bindings == null || bindings.size() != 1 || learningEvidenceService == null) return;
        String questionId = bindings.get(0).getQuestionId();
        Question question = questionId == null ? null : questionService.getById(questionId);
        if (question == null || question.getKnowledgePointId() == null || question.getKnowledgePointId().isBlank()) return;
        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("questionId", questionId);
        answer.put("studentAnswer", normalize(submission.getContent()));
        answer.put("correct", draft.score() >= 60);
        List<Map<String, Object>> answers = List.of(answer);
        Set<String> allowed = answers.stream().map(item -> String.valueOf(item.get("questionId")))
                .collect(java.util.stream.Collectors.toSet());
        learningEvidenceService.recordAiReviewedAnswers(submission.getStudentNo(), task.getCourseCode(),
                submission.getSubmissionId(), answers, allowed);
    }

    private ReviewDraft generateWithAgentic(String content, boolean hasFile, LearningTask task) {
        try {
            if (agenticClient.isMockMode() || !agenticClient.isConfiguredForRealAi()) {
                return null;
            }
            AgenticRequest request = new AgenticRequest();
            request.setCourseCode(task != null ? task.getCourseCode() : null);
            request.setContent(buildAgenticPrompt(content, hasFile, task));
            request.setContext(Map.of(
                    "taskNo", task != null ? normalize(task.getTaskNo()) : "",
                    "taskType", task != null ? normalize(task.getTaskType()) : "",
                    "taskDescription", task != null ? normalize(task.getDescription()) : "",
                    "rubric", rubricFor(task),
                    "questionAndReference", questionAndReference(task),
                    "submissionText", content == null ? "" : content,
                    "hasFile", hasFile,
                    "hasAttachment", hasFile
            ));
            AgenticResponse response = agenticClient.invoke("assessment", request);
            if (response == null || !response.isSuccess() || response.getData() == null || response.getData().isEmpty()) {
                return null;
            }
            String json = objectMapper.writeValueAsString(response.getData());
            return parseDraft(json);
        } catch (Exception e) {
            log.warn("Direct AI assessment failed; keep review pending. submission task={}", task != null ? task.getTaskNo() : "", e);
            return null;
        }
    }

    private String buildAgenticPrompt(String content, boolean hasFile, LearningTask task) {
        return """
                请对学生提交进行辅助评价，评价结果仅供教师复核参考。
                如果材料中包含填空题、简答题或编程题，请结合题干、参考答案、学生答案和得分点给出评分建议；填空题允许合理同义表达，但不要替代教师最终判断。
                任务类型：%s
                任务要求：%s
                题目与参考答案：
                %s
                评分标准：%s
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
                questionAndReference(task),
                task != null ? normalize(task.getGradingRule()) : "",
                hasFile ? "是" : "否",
                content.isBlank() ? "（无文字提交）" : content
        );
    }

    private String questionAndReference(LearningTask task) {
        if (task == null || taskQuestionService == null || questionService == null) return "（未绑定题目）";
        List<TaskQuestion> bindings = taskQuestionService.listByTaskNo(task.getTaskNo());
        if (bindings == null || bindings.isEmpty()) return "（未绑定题目）";
        List<String> ids = bindings.stream().map(TaskQuestion::getQuestionId)
                .filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (ids.isEmpty()) return "（未绑定题目）";
        Collection<Question> questions = questionService.listByIds(ids);
        if (questions == null || questions.isEmpty()) return "（题目不存在）";
        List<String> blocks = new ArrayList<>();
        for (Question question : questions) {
            blocks.add("题干：" + normalize(question.getStem())
                    + "\n参考答案：" + normalize(question.getAnswer())
                    + "\n知识点：" + normalize(question.getKnowledgePointId()));
        }
        return String.join("\n---\n", blocks);
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
        if (dimNode.isArray()) {
            dimNode.forEach(item -> {
                String name = normalize(item.path("name").asText());
                if (!name.isBlank()) dimensions.put(name, clamp(item.path("score").asInt(0)));
            });
        } else {
            dimensions.put("内容完整性", clamp(dimNode.path("内容完整性").asInt(0)));
            dimensions.put("知识点覆盖度", clamp(dimNode.path("知识点覆盖度").asInt(0)));
            dimensions.put("逻辑结构", clamp(dimNode.path("逻辑结构").asInt(0)));
            dimensions.put("表达规范", clamp(dimNode.path("表达规范").asInt(0)));
            dimensions.put("任务要求符合度", clamp(dimNode.path("任务要求符合度").asInt(0)));
        }
        // Keep the persisted review contract stable even if the Workflow omits a dimension
        // or uses a different ordering in its array output.
        dimensions.putIfAbsent("内容完整性", 0);
        dimensions.putIfAbsent("知识点覆盖度", 0);
        dimensions.putIfAbsent("逻辑结构", 0);
        dimensions.putIfAbsent("表达规范", 0);
        dimensions.putIfAbsent("任务要求符合度", 0);
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
        double confidence = root.has("confidence") ? Math.max(0D, Math.min(1D, root.path("confidence").asDouble())) : 0D;
        String basis = normalize(root.path("basis").asText("submission_review"));
        return new ReviewDraft(score, dimensions, summary, suggestions, riskLevel, confidence, basis);
    }

    private List<Map<String, Object>> rubricFor(LearningTask task) {
        if (task == null || normalize(task.getGradingRule()).isBlank()) return List.of();
        String gradingRule = normalize(task.getGradingRule());
        try {
            JsonNode node = objectMapper.readTree(gradingRule);
            if (node.isArray()) return objectMapper.convertValue(node, List.class);
            if (node.isObject()) return List.of(objectMapper.convertValue(node, Map.class));
        } catch (Exception ignored) {
            // Legacy tasks store grading rules as plain text.
        }
        return List.of(Map.of("description", gradingRule));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String buildSummary(int score, String content, boolean hasFile) {
        if (score >= 80) return "提交完成度较好，可作为教师复核参考。";
        if (score >= 60) return "提交具备基础内容，但仍有需要完善的部分。";
        return "提交完成度较低，建议教师结合原始答案重点复核。";
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
                               List<String> suggestions, String riskLevel, double confidence, String basis) {
    }
}

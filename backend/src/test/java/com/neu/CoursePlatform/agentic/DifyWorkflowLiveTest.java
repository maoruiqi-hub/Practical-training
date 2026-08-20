package com.neu.CoursePlatform.agentic;

import com.neu.CoursePlatform.dify.DifyClient;
import com.neu.CoursePlatform.dify.DifyKnowledgeService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 5 个 Dify Workflow 的真实联调验证（手动运行，会真实请求 api.dify.ai）。
 *
 * 与 AgenticClientDifyWorkflowTest 的区别：这里用真实的 DifyClient + 真实 key（从 backend/.env 读取），
 * 走 AgenticClient -> DifyClient -> /v1/workflows/run 的完整链路，确认后端能真正调用并解析 API。
 *
 * 注意：key 从 backend/.env 读取，不入库；无 key 时自动跳过（assumeTrue）。
 */
@Disabled("手动真实联调：会真实请求 api.dify.ai 并消耗额度；需要时去掉 @Disabled 单独运行")
class DifyWorkflowLiveTest {

    private AgenticClient liveClient;
    private Map<String, String> env;

    private void ensureClient() {
        if (liveClient != null) return;
        env = loadEnv();
        String cluster = env.get("DIFY_WORKFLOW_API_KEY_CLUSTER");
        assumeTrue(cluster != null && !cluster.isBlank(), "缺少 Dify key，跳过真实联调");

        DifyClient dify = new DifyClient();
        setField(dify, "baseUrl", env.getOrDefault("DIFY_BASE_URL", "https://api.dify.ai"));
        setField(dify, "workflowClusterKey", env.get("DIFY_WORKFLOW_API_KEY_CLUSTER"));
        setField(dify, "workflowSuggestionsKey", env.get("DIFY_WORKFLOW_API_KEY_SUGGESTIONS"));
        setField(dify, "workflowDiagnosisKey", env.get("DIFY_WORKFLOW_API_KEY_DIAGNOSIS"));
        setField(dify, "workflowAssessmentKey", env.get("DIFY_WORKFLOW_API_KEY_ASSESSMENT"));
        setField(dify, "workflowRecommendKey", env.get("DIFY_WORKFLOW_API_KEY_RECOMMEND"));

        DifyKnowledgeService knowledge = new DifyKnowledgeService(new DifyClient());
        liveClient = new AgenticClient(dify, knowledge);
        setField(liveClient, "mode", "dify");
    }

    @Test
    void clusterProblemsLive() throws Exception {
        ensureClient();
        String result = liveClient.clusterProblems(Map.of(
                "class_id", "C1", "course_id", "1",
                "mistakes", List.of(
                        Map.of("student_id", "S1", "knowledge_point_id", "KP1",
                                "knowledge_point_name", "链表指针操作", "mistake_rate", 0.85),
                        Map.of("student_id", "S2", "knowledge_point_id", "KP1",
                                "knowledge_point_name", "链表指针操作", "mistake_rate", 0.85),
                        Map.of("student_id", "S3", "knowledge_point_id", "KP1",
                                "knowledge_point_name", "链表指针操作", "mistake_rate", 0.85),
                        Map.of("student_id", "S4", "knowledge_point_id", "KP1",
                                "knowledge_point_name", "链表指针操作", "mistake_rate", 0.85),
                        Map.of("student_id", "S5", "knowledge_point_id", "KP1",
                                "knowledge_point_name", "链表指针操作", "mistake_rate", 0.85),
                        Map.of("student_id", "S6", "knowledge_point_id", "KP1",
                                "knowledge_point_name", "链表指针操作", "mistake_rate", 0.85),
                        Map.of("student_id", "S7", "knowledge_point_id", "KP2",
                                "knowledge_point_name", "异常处理分支", "mistake_rate", 0.75),
                        Map.of("student_id", "S8", "knowledge_point_id", "KP2",
                                "knowledge_point_name", "异常处理分支", "mistake_rate", 0.75)
                ),
                "feedbacks", List.of(
                        Map.of("knowledge_point_id", "KP1", "pattern", "删除节点后未更新前驱指针"),
                        Map.of("knowledge_point_id", "KP2", "pattern", "未覆盖异常分支")
                ),
                "questions", List.of(
                        Map.of("question_id", "Q101", "knowledge_point_id", "KP1",
                                "wrong_count", 6, "wrong_rate", 0.75,
                                "common_wrong_patterns", List.of("前驱指针未更新", "空链表边界未处理")),
                        Map.of("question_id", "Q102", "knowledge_point_id", "KP2",
                                "wrong_count", 5, "wrong_rate", 0.625,
                                "common_wrong_patterns", List.of("异常类型判断错误"))
                )));
        System.out.println("[clusterProblems] => " + result);
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void teachingSuggestionsLive() throws Exception {
        ensureClient();
        String result = liveClient.teachingSuggestions(Map.of(
                "class_id", "C1", "course_id", "1",
                "weak_points", List.of(
                        Map.of("name", "异常处理", "score_rate", 0.35),
                        Map.of("name", "文件读写", "score_rate", 0.48)),
                "clusters", List.of(
                        Map.of("topic", "异常类型判断错误", "student_count", 9,
                                "knowledge_point_ids", List.of("KP2"), "confidence", 0.9)),
                "progress_data", Map.of("avg_completion_rate", 0.52),
                "active_risk_count", 8, "student_count", 30));
        System.out.println("[teachingSuggestions] => " + result);
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void towerDiagnosisLive() {
        ensureClient();
        AgenticRequest req = new AgenticRequest();
        req.setCourseCode("1");
        req.setContext(Map.of(
                "knowledgePointId", "KP1", "roomType", "battle", "correctRate", 0.5,
                "cleared", false,
                "answers", List.of(
                        Map.of("questionId", "Q1", "knowledgePointId", "KP1",
                                "studentAnswer", "删除 head 后未更新 next", "correctAnswer", "更新前驱节点的 next",
                                "isCorrect", false, "errorSummary", "删除头节点后链表断裂"),
                        Map.of("questionId", "Q2", "knowledgePointId", "KP1",
                                "studentAnswer", "直接访问 current.next", "correctAnswer", "先判断 current 非空",
                                "isCorrect", false, "errorSummary", "未处理空链表边界"),
                        Map.of("questionId", "Q3", "knowledgePointId", "KP1",
                                "studentAnswer", "遍历并返回长度", "correctAnswer", "遍历并返回长度",
                                "isCorrect", true, "errorSummary", "") )));
        AgenticResponse resp = liveClient.invoke("tower-diagnosis-report", req);
        System.out.println("[tower-diagnosis-report] => " + resp.getData());
        assertTrue(resp.isSuccess());
        assertFalse(resp.getData().isEmpty());
    }

    @Test
    void assessmentLive() {
        ensureClient();
        AgenticRequest req = new AgenticRequest();
        req.setCourseCode("1");
        req.setContext(Map.of(
                "taskNo", "T1", "taskType", "essay", "taskDescription", "写实验报告",
                "rubric", List.of(
                        Map.of("name", "问题分析", "points", 30),
                        Map.of("name", "实验过程", "points", 30),
                        Map.of("name", "结果与讨论", "points", 25),
                        Map.of("name", "表达规范", "points", 15)),
                "submissionText", "本实验比较了两种链表遍历方法。实验过程包含输入数据、运行步骤和结果截图。结果显示迭代方法在大规模数据下更稳定，但报告对异常情况的讨论不够充分。",
                "hasAttachment", false));
        AgenticResponse resp = liveClient.invoke("assessment", req);
        System.out.println("[assessment] => " + resp.getData());
        assertTrue(resp.isSuccess());
        assertFalse(resp.getData().isEmpty());
    }

    @Test
    void recommendLive() {
        ensureClient();
        AgenticRequest req = new AgenticRequest();
        req.setCourseCode("1");
        req.setContext(Map.of(
                "student", Map.of("anonymousId", "2024001"),
                "recommendations", List.of(
                        Map.of("targetId", "ap-1", "targetName", "链表与指针基础",
                                "score", 30, "type", "review_material", "priority", 1),
                        Map.of("targetId", "ap-2", "targetName", "异常处理综合练习",
                                "score", 42, "type", "practice", "priority", 2),
                        Map.of("targetId", "ap-3", "targetName", "文件读写项目",
                                "score", 58, "type", "extended_material", "priority", 3))));
        AgenticResponse resp = liveClient.invoke("recommend", req);
        System.out.println("[recommend] => " + resp.getData());
        assertTrue(resp.isSuccess());
        assertFalse(resp.getData().isEmpty());
    }

    // ---- helpers ----

    private static Map<String, String> loadEnv() {
        Map<String, String> env = new HashMap<>();
        for (Path p : List.of(Path.of(".env"), Path.of("backend/.env"))) {
            if (Files.exists(p)) {
                try {
                    for (String line : Files.readAllLines(p)) {
                        String t = line.trim();
                        if (t.isEmpty() || t.startsWith("#")) continue;
                        int eq = t.indexOf('=');
                        if (eq > 0) env.put(t.substring(0, eq).trim(), t.substring(eq + 1).trim());
                    }
                } catch (Exception e) {
                    throw new RuntimeException("读取 .env 失败: " + p, e);
                }
                break;
            }
        }
        return env;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }
}

package com.neu.CoursePlatform.agentic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.dify.DifyClient;
import com.neu.CoursePlatform.dify.DifyKnowledgeService;
import com.neu.CoursePlatform.dify.DifyResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 5 个已迁移 Dify Workflow 能力的后端调用集成测试。
 *
 * 验证 AgenticClient 在 dify 模式下：
 * 1. 按 capability 路由到正确的 Workflow（capability -> runWorkflow(capability, ...)）；
 * 2. 组装出符合 Dify 输入 schema 的 request_json；
 * 3. 解包 data.outputs 并返回结构化结果。
 *
 * 5 个能力：clusterProblems / teachingSuggestions / tower-diagnosis-report /
 * assessment / recommend（riskDetect 保留后端规则引擎，不在本测试范围）。
 */
class AgenticClientDifyWorkflowTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 记录 runWorkflow(capability, inputs) 的调用参数，返回可配置的结构化响应。 */
    private static class CapturingDifyClient extends DifyClient {
        String lastCapability;
        Map<String, Object> lastInputs;
        DifyResponse response = DifyResponse.success("{\"result\":{\"summary\":\"ok\"}}");

        @Override
        public boolean isConfigured() { return true; }

        @Override
        public boolean isWorkflowConfigured() { return true; }

        @Override
        public DifyResponse runWorkflow(String capability, Map<String, Object> inputs) {
            this.lastCapability = capability;
            this.lastInputs = inputs;
            return response;
        }
    }

    private static AgenticClient clientFor(CapturingDifyClient dify) {
        DifyKnowledgeService knowledge = new DifyKnowledgeService(new DifyClient());
        AgenticClient client = new AgenticClient(dify, knowledge);
        setField(client, "mode", "dify");
        return client;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requestJson(CapturingDifyClient dify) throws Exception {
        String raw = (String) dify.lastInputs.get("request_json");
        return MAPPER.readValue(raw, Map.class);
    }

    // ============ 1. clusterProblems（错题聚类）============

    @Test
    void clusterProblemsRoutesAndAggregatesMistakes() throws Exception {
        CapturingDifyClient dify = new CapturingDifyClient();
        AgenticClient client = clientFor(dify);

        Map<String, Object> request = Map.of(
                "class_id", "C1",
                "course_id", "1",
                "mistakes", List.of(
                        Map.of("student_id", "S1", "knowledge_point_id", "KP1",
                                "knowledge_point_name", "链表", "mistake_rate", 0.5),
                        Map.of("student_id", "S2", "knowledge_point_id", "KP1",
                                "knowledge_point_name", "链表", "mistake_rate", 0.5),
                        Map.of("student_id", "S1", "knowledge_point_id", "KP2",
                                "knowledge_point_name", "栈", "mistake_rate", 0.3)
                ),
                "feedbacks", List.of(),
                "questions", List.of(Map.of(
                        "question_id", "Q1",
                        "knowledge_point_id", "KP1",
                        "wrong_count", 2,
                        "wrong_rate", 0.5,
                        "common_wrong_patterns", List.of("前驱指针未更新")))
        );
        String result = client.clusterProblems(request);

        assertEquals("clusterProblems", dify.lastCapability);
        Map<String, Object> json = requestJson(dify);
        assertEquals("1", json.get("courseCode"));

        List<?> kps = (List<?>) json.get("knowledgePoints");
        assertEquals(2, kps.size());
        Map<?, ?> kp1 = (Map<?, ?>) kps.get(0);
        assertEquals("KP1", kp1.get("id"));
        assertEquals(2, ((Number) kp1.get("studentCount")).intValue());
        assertEquals(1, ((Number) kp1.get("wrongCount")).intValue()); // round(0.5 * 2)

        List<?> questionStats = (List<?>) json.get("questionStats");
        assertEquals(1, questionStats.size());
        Map<?, ?> question = (Map<?, ?>) questionStats.get(0);
        assertEquals("Q1", question.get("questionId"));
        assertEquals("KP1", question.get("knowledgePointId"));
        assertEquals(2, ((Number) question.get("wrongCount")).intValue());

        assertTrue(result.contains("summary")); // 解包 result 后返回
    }

    // ============ 2. teachingSuggestions（教学建议，班级版）============

    @Test
    void teachingSuggestionsRoutesClassSummary() throws Exception {
        CapturingDifyClient dify = new CapturingDifyClient();
        AgenticClient client = clientFor(dify);

        Map<String, Object> request = Map.of(
                "class_id", "C1",
                "course_id", "1",
                "weak_points", List.of(Map.of("name", "链表", "score_rate", 0.6)),
                "clusters", List.of(),
                "progress_data", Map.of("avg_completion_rate", 0.75),
                "active_risk_count", 3,
                "student_count", 30
        );
        client.teachingSuggestions(request);

        assertEquals("teachingSuggestions", dify.lastCapability);
        Map<String, Object> json = requestJson(dify);
        assertEquals("1", json.get("courseCode"));

        Map<?, ?> summary = (Map<?, ?>) json.get("classSummary");
        assertEquals(30, ((Number) summary.get("studentCount")).intValue());
        assertEquals(3, ((Number) summary.get("atRiskStudentCount")).intValue());
        assertEquals(0.75, ((Number) summary.get("completionRate")).doubleValue(), 0.001);
        assertEquals(60.0, ((Number) summary.get("averageMastery")).doubleValue(), 0.001);

        List<?> weakPoints = (List<?>) json.get("weakPoints");
        assertEquals(1, weakPoints.size());
        Map<?, ?> wp = (Map<?, ?>) weakPoints.get(0);
        assertEquals("链表", wp.get("knowledgePointId"));
        assertEquals(0.4, ((Number) wp.get("wrongRate")).doubleValue(), 0.001);
    }

    // ============ 3. tower-diagnosis-report（爬塔诊断）============

    @Test
    void towerDiagnosisRoutesAndCountsQuestions() throws Exception {
        CapturingDifyClient dify = new CapturingDifyClient();
        AgenticClient client = clientFor(dify);

        AgenticRequest req = new AgenticRequest();
        req.setCourseCode("1");
        req.setContext(Map.of(
                "knowledgePointId", "KP1",
                "roomType", "battle",
                "correctRate", 0.7,
                "cleared", false,
                "answers", List.of("a", "b", "c")
        ));
        AgenticResponse resp = client.invoke("tower-diagnosis-report", req);

        assertEquals("tower-diagnosis-report", dify.lastCapability);
        Map<String, Object> json = requestJson(dify);
        assertEquals("1", json.get("courseCode"));
        assertEquals("KP1", json.get("knowledgePointId"));
        assertEquals("battle", json.get("roomType"));
        assertEquals(3, ((Number) json.get("questionCount")).intValue());
        assertEquals(3, ((List<?>) json.get("answers")).size());

        assertTrue(resp.isSuccess());
        assertEquals("ok", resp.getData().get("summary")); // 解包 result
    }

    // ============ 4. assessment（作业评阅）============

    @Test
    void assessmentRoutesTaskAndSubmission() throws Exception {
        CapturingDifyClient dify = new CapturingDifyClient();
        AgenticClient client = clientFor(dify);

        AgenticRequest req = new AgenticRequest();
        req.setCourseCode("1");
        req.setContext(Map.of(
                "taskNo", "T1",
                "taskType", "essay",
                "taskDescription", "写实验报告",
                "rubric", List.of(),
                "submissionText", "我的提交内容",
                "hasAttachment", false
        ));
        AgenticResponse resp = client.invoke("assessment", req);

        assertEquals("assessment", dify.lastCapability);
        Map<String, Object> json = requestJson(dify);
        assertEquals("1", json.get("courseCode"));

        Map<?, ?> task = (Map<?, ?>) json.get("task");
        assertEquals("T1", task.get("taskNo"));
        assertEquals("essay", task.get("taskType"));
        assertEquals("写实验报告", task.get("description"));

        Map<?, ?> submission = (Map<?, ?>) json.get("submission");
        assertEquals("我的提交内容", submission.get("text"));
        assertEquals(false, submission.get("hasAttachment"));

        assertTrue(resp.isSuccess());
    }

    // ============ 5. recommend（推荐理由，批量）============

    @Test
    void recommendRoutesBatchRecommendations() throws Exception {
        CapturingDifyClient dify = new CapturingDifyClient();
        AgenticClient client = clientFor(dify);

        AgenticRequest req = new AgenticRequest();
        req.setCourseCode("1");
        req.setContext(Map.of(
                "student", Map.of("anonymousId", "2024001"),
                "recommendations", List.of(Map.of(
                        "targetId", "ap-1",
                        "targetName", "数据结构",
                        "score", 30,
                        "type", "review_material",
                        "priority", 1
                ))
        ));
        AgenticResponse resp = client.invoke("recommend", req);

        assertEquals("recommend", dify.lastCapability);
        Map<String, Object> json = requestJson(dify);
        assertEquals("1", json.get("courseCode"));

        Map<?, ?> student = (Map<?, ?>) json.get("student");
        assertEquals("2024001", student.get("anonymousId"));
        assertEquals(1, ((List<?>) json.get("recommendations")).size());

        assertTrue(resp.isSuccess());
    }

    // ============ helper ============

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

package com.neu.CoursePlatform.agentic;

import com.neu.CoursePlatform.dify.DifyClient;
import com.neu.CoursePlatform.dify.DifyKnowledgeService;
import com.neu.CoursePlatform.dify.DifyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgenticClient 单元测试 — 多模式 AI 客户端。
 * 覆盖 mock/dify/deepseek/http 四种模式的调用路径、模式选择逻辑、
 * 错误处理和各种 capability 的 mock 返回。
 */
class AgenticClientTest {

    private AgenticClient client;
    private DifyClient mockDifyClient;
    private DifyKnowledgeService mockKnowledgeService;

    @BeforeEach
    void setUp() {
        mockDifyClient = createConfiguredDifyClient();
        mockKnowledgeService = createMockKnowledgeService();
        client = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setMode("mock"); // 默认使用 mock 模式
    }

    // ============ invoke() - Mock 模式 ============

    @Test
    void invokeReturnsMockResponseForLecture() {
        AgenticRequest req = createRequest("CS101", "什么是多态？");
        AgenticResponse resp = client.invoke("lecture", req);
        assertTrue(resp.isSuccess());
        assertNotNull(resp.getData());
        assertTrue(resp.getData().containsKey("mock"));
        assertEquals("lecture", resp.getData().get("capability"));
    }

    @Test
    void invokeReturnsMockResponseForQa() {
        AgenticRequest req = createRequest("CS101", "Java和Python有什么区别？");
        AgenticResponse resp = client.invoke("qa", req);
        assertTrue(resp.isSuccess());
        assertNotNull(resp.getData());
        assertEquals("qa", resp.getData().get("capability"));
    }

    @Test
    void invokeReturnsMockResponseForAssessment() {
        AgenticRequest req = createRequest("CS101", "评估作业");
        AgenticResponse resp = client.invoke("assessment", req);
        assertTrue(resp.isSuccess());
        assertEquals("assessment", resp.getData().get("capability"));
    }

    @Test
    void invokeReturnsMockResponseForUnknownCapability() {
        AgenticRequest req = createRequest("CS101", "任意内容");
        AgenticResponse resp = client.invoke("unknown-capability", req);
        assertTrue(resp.isSuccess()); // mock 模式永远返回成功
    }

    // ============ 各 capability 专项方法 - Mock 模式 ============

    @Test
    void clusterProblemsReturnsMockJson() throws Exception {
        String result = client.clusterProblems(Map.of("data", "test"));
        assertNotNull(result);
        assertTrue(result.contains("Mock聚类示例") || result.contains("topic"));
    }

    @Test
    void teachingSuggestionsReturnsMockJson() throws Exception {
        String result = client.teachingSuggestions(Map.of("data", "test"));
        assertNotNull(result);
        assertTrue(result.contains("Mock模式建议") || result.contains("suggestion_type"));
    }

    @Test
    void knowledgeExtractInDifyModeReturnsContent() throws Exception {
        // knowledgeExtract 没有 mock 分支，直接走 dify/deepseek/http
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "dify");
        String result = localClient.knowledgeExtract(Map.of("data", "test"));
        assertNotNull(result);
        assertTrue(result.contains("Dify mock success"));
    }

    @Test
    void chatInDifyModeReturnsContent() throws Exception {
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "dify");
        String result = localClient.chat(Map.of("content", "请问什么是设计模式？"));
        assertNotNull(result);
        assertTrue(result.contains("Dify mock success"));
    }

    @Test
    void gradeInDifyModeReturnsContent() throws Exception {
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "dify");
        String result = localClient.grade(Map.of("submission", "学生作业内容"));
        assertNotNull(result);
        assertTrue(result.contains("Dify mock success"));
    }

    @Test
    void recommendInDifyModeReturnsContent() throws Exception {
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "dify");
        String result = localClient.recommend(Map.of("student", "student-1"));
        assertNotNull(result);
        assertTrue(result.contains("Dify mock success"));
    }

    @Test
    void riskDetectInDifyModeReturnsContent() throws Exception {
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "dify");
        String result = localClient.riskDetect(Map.of("data", "学习行为数据"));
        assertNotNull(result);
        assertTrue(result.contains("Dify mock success"));
    }

    // ============ 模式选择逻辑 ============

    @Test
    void mockModeIsCaseInsensitive() {
        AgenticRequest req = createRequest("CS101", "test");
        setMode("MOCK");
        AgenticResponse resp = client.invoke("lecture", req);
        assertTrue(resp.isSuccess());
        assertTrue(resp.getData().containsKey("mock"));
    }

    @Test
    void difyModeWhenConfiguredReturnsResponse() {
        AgenticRequest req = createRequest("CS101", "测试问题");
        setMode("dify");
        setField(client, "difyClient", mockDifyClient);

        AgenticResponse resp = client.invoke("qa", req);
        // DifyClient 返回成功，所以应该成功
        assertTrue(resp.isSuccess());
    }

    @Test
    void deepseekModeGracefullyFailsWhenUnreachable() {
        setMode("deepseek");
        setField(client, "baseUrl", "http://localhost:19999");
        setField(client, "apiKey", "sk-test-key");

        AgenticRequest req = createRequest("CS101", "test");
        AgenticResponse resp = client.invoke("lecture", req);
        // 连接失败时应该降级返回 unavailable
        assertFalse(resp.isSuccess());
        assertEquals("Agentic 服务不可用", resp.getMessage());
    }

    @Test
    void httpModeGracefullyFailsWhenUnreachable() {
        setMode("http");
        setField(client, "baseUrl", "http://localhost:19999");

        AgenticRequest req = createRequest("CS101", "test");
        AgenticResponse resp = client.invoke("lecture", req);
        assertFalse(resp.isSuccess());
    }

    @Test
    void unknownModeReturnsUnavailable() {
        setMode("unknown_mode");
        AgenticRequest req = createRequest("CS101", "test");
        AgenticResponse resp = client.invoke("lecture", req);
        assertFalse(resp.isSuccess());
    }

    @Test
    void emptyModeFallsToHttpAndFails() {
        setMode("");
        AgenticRequest req = createRequest("CS101", "test");
        AgenticResponse resp = client.invoke("lecture", req);
        // 空字符串不等于 "dify"/"deepseek"/"http"/"mock"，走 http 路径并失败
        assertFalse(resp.isSuccess());
    }

    // ============ Dify 模式 - 详细场景 ============

    @Test
    void invokeDifyFallsBackWhenDifyNotConfigured() {
        DifyClient unconfigured = createUnconfiguredDifyClient();
        AgenticClient localClient = new AgenticClient(unconfigured, mockKnowledgeService);
        setField(localClient, "mode", "dify");

        AgenticRequest req = createRequest("CS101", "test");
        AgenticResponse resp = localClient.invoke("lecture", req);
        // 应该回退到 mock
        assertTrue(resp.isSuccess());
        assertTrue(resp.getData().containsKey("fallback"));
    }

    @Test
    void invokeDifyReturnsUnavailableWhenDifyFails() {
        DifyClient failingClient = createFailingDifyClient();
        AgenticClient localClient = new AgenticClient(failingClient, mockKnowledgeService);
        setField(localClient, "mode", "dify");

        AgenticRequest req = createRequest("CS101", "test");
        AgenticResponse resp = localClient.invoke("qa", req);
        assertFalse(resp.isSuccess());
    }

    // ============ Dify 模式 - 专项方法 ============

    @Test
    void clusterProblemsInDifyModeReturnsContent() throws Exception {
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "dify");

        String result = localClient.clusterProblems(Map.of("data", "错题数据"));
        assertNotNull(result);
        assertTrue(result.contains("Dify mock success"));
    }

    @Test
    void clusterProblemsInDifyModeThrowsOnError() {
        DifyClient failingClient = createFailingDifyClient();
        AgenticClient localClient = new AgenticClient(failingClient, mockKnowledgeService);
        setField(localClient, "mode", "dify");

        assertThrows(AgenticClient.AgenticException.class,
                () -> localClient.clusterProblems(Map.of("data", "test")));
    }

    @Test
    void teachingSuggestionsInDifyModeThrowsOnError() {
        DifyClient failingClient = createFailingDifyClient();
        AgenticClient localClient = new AgenticClient(failingClient, mockKnowledgeService);
        setField(localClient, "mode", "dify");

        assertThrows(AgenticClient.AgenticException.class,
                () -> localClient.teachingSuggestions(Map.of("data", "test")));
    }

    @Test
    void knowledgeExtractInDifyModeThrowsOnError() {
        DifyClient failingClient = createFailingDifyClient();
        AgenticClient localClient = new AgenticClient(failingClient, mockKnowledgeService);
        setField(localClient, "mode", "dify");

        assertThrows(AgenticClient.AgenticException.class,
                () -> localClient.knowledgeExtract(Map.of("data", "test")));
    }

    // ============ DeepSeek 模式 - 专项方法 ============

    @Test
    void clusterProblemsInDeepSeekModeThrowsOnUnreachable() {
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "deepseek");
        setField(localClient, "baseUrl", "http://localhost:19999");
        setField(localClient, "apiKey", "sk-key");

        assertThrows(AgenticClient.AgenticException.class,
                () -> localClient.clusterProblems(Map.of("data", "test")));
    }

    @Test
    void teachingSuggestionsInDeepSeekModeThrowsOnUnreachable() {
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "deepseek");
        setField(localClient, "baseUrl", "http://localhost:19999");
        setField(localClient, "apiKey", "sk-key");

        assertThrows(AgenticClient.AgenticException.class,
                () -> localClient.teachingSuggestions(Map.of("data", "test")));
    }

    // ============ AgenticException ============

    @Test
    void agenticExceptionWithMessage() {
        AgenticClient.AgenticException ex = new AgenticClient.AgenticException("服务调用失败");
        assertEquals("服务调用失败", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void agenticExceptionWithCause() {
        RuntimeException cause = new RuntimeException("网络超时");
        AgenticClient.AgenticException ex = new AgenticClient.AgenticException("服务调用失败", cause);
        assertEquals("服务调用失败", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    // ============ HTTP 模式 - 专项方法异常路径 ============

    @Test
    void clusterProblemsInHttpModeThrowsOnUnreachable() {
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "http");
        setField(localClient, "baseUrl", "http://localhost:19999");

        assertThrows(AgenticClient.AgenticException.class,
                () -> localClient.clusterProblems(Map.of("data", "test")));
    }

    @Test
    void chatInHttpModeThrowsOnUnreachable() {
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "http");
        setField(localClient, "baseUrl", "http://localhost:19999");

        assertThrows(AgenticClient.AgenticException.class,
                () -> localClient.chat(Map.of("content", "test")));
    }

    @Test
    void gradeInHttpModeThrowsOnUnreachable() {
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "http");
        setField(localClient, "baseUrl", "http://localhost:19999");

        assertThrows(AgenticClient.AgenticException.class,
                () -> localClient.grade(Map.of("data", "test")));
    }

    @Test
    void recommendInHttpModeThrowsOnUnreachable() {
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "http");
        setField(localClient, "baseUrl", "http://localhost:19999");

        assertThrows(AgenticClient.AgenticException.class,
                () -> localClient.recommend(Map.of("data", "test")));
    }

    @Test
    void riskDetectInHttpModeThrowsOnUnreachable() {
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "http");
        setField(localClient, "baseUrl", "http://localhost:19999");

        assertThrows(AgenticClient.AgenticException.class,
                () -> localClient.riskDetect(Map.of("data", "test")));
    }

    // ============ workflowOutputsMap 解包（result vs structured_output）============

    @Test
    void workflowOutputsMapUnwrapsResultKey() throws Exception {
        Object result = invokePrivate(client, "workflowOutputsMap",
                DifyResponse.success("{\"result\":{\"summary\":\"ok\"}}"));
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("ok", map.get("summary"));
    }

    @Test
    void workflowOutputsMapUnwrapsStructuredOutputKey() throws Exception {
        Object result = invokePrivate(client, "workflowOutputsMap",
                DifyResponse.success("{\"structured_output\":{\"summary\":\"ok2\"}}"));
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("ok2", map.get("summary"));
    }

    @Test
    void workflowOutputsMapUnwrapsResultAsJsonString() throws Exception {
        Object result = invokePrivate(client, "workflowOutputsMap",
                DifyResponse.success("{\"result\":\"{\\\"summary\\\":\\\"ok3\\\"}\"}"));
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("ok3", map.get("summary"));
    }

    @Test
    void workflowOutputsMapKeepsPlainMap() throws Exception {
        Object result = invokePrivate(client, "workflowOutputsMap",
                DifyResponse.success("{\"a\":1,\"b\":2}"));
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
    }

    // ============ helper ============

    private static AgenticRequest createRequest(String courseCode, String content) {
        AgenticRequest req = new AgenticRequest();
        req.setCourseCode(courseCode);
        req.setContent(content);
        req.setContext(Map.of("userId", "test-user"));
        return req;
    }

    /** 返回成功响应的已配置 DifyClient */
    private static DifyClient createConfiguredDifyClient() {
        return new DifyClient() {
            @Override
            public boolean isConfigured() { return true; }

            @Override
            public DifyResponse sendChatMessage(String query, String userId) {
                return DifyResponse.success("Dify mock success: " + query);
            }

            @Override
            public DifyResponse sendCompletion(String query, String userId) {
                return DifyResponse.success("Dify completion success");
            }

            @Override
            public DifyResponse runWorkflow(java.util.Map<String, Object> inputs, String userId) {
                return DifyResponse.success("Dify workflow success");
            }

            @Override
            public DifyResponse runWorkflow(String capability, java.util.Map<String, Object> inputs) {
                return DifyResponse.success("Dify mock success");
            }

            @Override
            public DifyResponse retrieveKnowledge(String query, int topK) {
                return DifyResponse.success("[{\"content\":\"test\",\"score\":0.9}]");
            }
        };
    }

    /** 返回 isConfigured() = false 的 DifyClient */
    private static DifyClient createUnconfiguredDifyClient() {
        return new DifyClient() {
            @Override
            public boolean isConfigured() { return false; }
        };
    }

    /** 返回 error 响应的 DifyClient */
    private static DifyClient createFailingDifyClient() {
        return new DifyClient() {
            @Override
            public boolean isConfigured() { return true; }

            @Override
            public DifyResponse sendChatMessage(String query, String userId) {
                return DifyResponse.error("Dify error: service unavailable");
            }

            @Override
            public DifyResponse sendCompletion(String query, String userId) {
                return DifyResponse.error("Dify error: service unavailable");
            }

            @Override
            public DifyResponse runWorkflow(java.util.Map<String, Object> inputs, String userId) {
                return DifyResponse.error("Dify error: service unavailable");
            }

            @Override
            public DifyResponse runWorkflow(String capability, java.util.Map<String, Object> inputs) {
                return DifyResponse.error("Dify error: service unavailable");
            }

            @Override
            public DifyResponse retrieveKnowledge(String query, int topK) {
                return DifyResponse.error("Knowledge base error");
            }
        };
    }

    private static DifyKnowledgeService createMockKnowledgeService() {
        return new DifyKnowledgeService(new DifyClient() {
            @Override
            public boolean isConfigured() { return false; }
        }) {
            @Override
            public String buildRagPrompt(String query, String courseCode, String knowledgePoint) {
                return "[RAG prompt] " + query;
            }
        };
    }

    private void setMode(String mode) {
        setField(client, "mode", mode);
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

    private static Object invokePrivate(Object target, String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }
            var method = target.getClass().getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke " + methodName, e);
        }
    }
}

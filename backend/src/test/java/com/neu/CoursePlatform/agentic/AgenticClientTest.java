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

    // ============ DeepSeek 模式 - 配置检查 ============

    @Test
    void isConfiguredForRealAiReturnsTrueForDeepseekModeWithValidConfig() {
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "deepseek");
        setField(localClient, "apiKey", "sk-test-key");
        setField(localClient, "baseUrl", "https://api.deepseek.com");
        assertTrue(localClient.isConfiguredForRealAi());
    }

    @Test
    void isConfiguredForRealAiReturnsFalseForDeepseekModeWithoutApiKey() {
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "deepseek");
        setField(localClient, "apiKey", "");
        setField(localClient, "baseUrl", "https://api.deepseek.com");
        assertFalse(localClient.isConfiguredForRealAi());
    }

    @Test
    void isConfiguredForRealAiReturnsFalseForDeepseekModeWithoutBaseUrl() {
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "deepseek");
        setField(localClient, "apiKey", "sk-test-key");
        setField(localClient, "baseUrl", "");
        assertFalse(localClient.isConfiguredForRealAi());
    }

    @Test
    void isConfiguredForRealAiReturnsTrueForDifyModeWhenConfigured() {
        AgenticClient localClient = new AgenticClient(mockDifyClient, mockKnowledgeService);
        setField(localClient, "mode", "dify");
        assertTrue(localClient.isConfiguredForRealAi());
    }

    @Test
    void isConfiguredForRealAiReturnsFalseForMockMode() {
        assertFalse(client.isConfiguredForRealAi());
    }

    @Test
    void configurationMessageReturnsMockModeMessage() {
        assertEquals("当前为 mock 模式，仅用于开发联调", client.configurationMessage());
    }

    @Test
    void configurationMessageReturnsDeepseekMessage() {
        setMode("deepseek");
        assertEquals("DeepSeek/Anthropic API token 或 base-url 未配置", client.configurationMessage());
    }

    @Test
    void configurationMessageReturnsDifyMessage() {
        setMode("dify");
        assertEquals("Dify API key 或服务地址未配置", client.configurationMessage());
    }

    @Test
    void configurationMessageReturnsHttpMessage() {
        setMode("http");
        assertEquals("外部 agentic 服务地址未配置", client.configurationMessage());
    }

    @Test
    void isMockModeReturnsTrueForMockMode() {
        assertTrue(client.isMockMode());
        setMode("deepseek");
        assertFalse(client.isMockMode());
    }

    // ============ DeepSeek JSON 响应解析 (extractJson) ============

    @Test
    void extractJsonParsesRawJsonObject() throws Exception {
        String raw = "{\"name\":\"test\",\"value\":123}";
        String result = invokeExtractJson(raw);
        assertEquals("{\"name\":\"test\",\"value\":123}", result);
    }

    @Test
    void extractJsonParsesRawJsonArray() throws Exception {
        String raw = "[{\"name\":\"item1\"},{\"name\":\"item2\"}]";
        String result = invokeExtractJson(raw);
        assertEquals("[{\"name\":\"item1\"},{\"name\":\"item2\"}]", result);
    }

    @Test
    void extractJsonStripsMarkdownCodeBlock() throws Exception {
        String raw = "```json\n{\"name\":\"test\"}\n```";
        String result = invokeExtractJson(raw);
        assertEquals("{\"name\":\"test\"}", result);
    }

    @Test
    void extractJsonStripsMarkdownCodeBlockWithoutLang() throws Exception {
        String raw = "```\n[1,2,3]\n```";
        String result = invokeExtractJson(raw);
        assertEquals("[1,2,3]", result);
    }

    @Test
    void extractJsonExtractsJsonFromWrappedText() throws Exception {
        String raw = "这是响应内容 {\"data\": \"hello\"} 后续文本";
        String result = invokeExtractJson(raw);
        assertEquals("{\"data\": \"hello\"}", result);
    }

    @Test
    void extractJsonReturnsOriginalWhenNoJsonFound() throws Exception {
        String raw = "这是纯文本响应，不含JSON";
        String result = invokeExtractJson(raw);
        assertEquals("这是纯文本响应，不含JSON", result);
    }

    // ============ DeepSeek 响应解析 (parseDeepSeekResponse) ============

    @Test
    void parseDeepSeekResponseParsesJsonObject() throws Exception {
        String raw = "{\"result\":\"success\",\"score\":85}";
        Map<String, Object> data = invokeParseResponse("lecture", raw);
        assertEquals("success", data.get("result"));
        assertEquals(85, data.get("score"));
    }

    @Test
    void parseDeepSeekResponseParsesJsonArray() throws Exception {
        String raw = "[{\"topic\":\"主题1\"},{\"topic\":\"主题2\"}]";
        Map<String, Object> data = invokeParseResponse("qa", raw);
        assertTrue(data.containsKey("items"));
        assertTrue(data.get("items") instanceof java.util.List);
        assertEquals(2, ((java.util.List<?>) data.get("items")).size());
    }

    @Test
    void parseDeepSeekResponseHandlesMarkdownWrappedJson() throws Exception {
        String raw = "```json\n{\"key\":\"value\"}\n```";
        Map<String, Object> data = invokeParseResponse("lecture", raw);
        assertEquals("value", data.get("key"));
    }

    @Test
    void parseDeepSeekResponseReturnsResultKeyForPlainText() throws Exception {
        String raw = "这是普通文本回答";
        Map<String, Object> data = invokeParseResponse("lecture", raw);
        assertTrue(data.containsKey("result"));
        assertEquals("这是普通文本回答", data.get("result"));
    }

    // ============ AgenticRequest / AgenticResponse ============

    @Test
    void agenticRequestBuilderSetsAllFields() {
        AgenticRequest req = new AgenticRequest();
        req.setCourseCode("CS102");
        req.setContent("测试内容");
        req.setKnowledgePointId("kp-5");
        req.setContext(Map.of("key", "val"));
        assertEquals("CS102", req.getCourseCode());
        assertEquals("测试内容", req.getContent());
        assertEquals("kp-5", req.getKnowledgePointId());
        assertEquals(Map.of("key", "val"), req.getContext());
    }

    @Test
    void agenticResponseBuilderSetsAllFields() {
        AgenticResponse resp = new AgenticResponse(true, Map.of("data", "test"), "ok");
        assertTrue(resp.isSuccess());
        assertEquals("ok", resp.getMessage());
        assertEquals(Map.of("data", "test"), resp.getData());
    }

    @Test
    void agenticResponseUnavailableCreatesErrorResponse() {
        AgenticResponse resp = AgenticResponse.unavailable();
        assertFalse(resp.isSuccess());
        assertEquals("Agentic 服务不可用", resp.getMessage());
        assertNotNull(resp.getData());
        assertTrue(resp.getData().isEmpty());
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

    /** 反射调用 AgenticClient.extractJson(String) */
    private String invokeExtractJson(String raw) throws Exception {
        java.lang.reflect.Method method = AgenticClient.class.getDeclaredMethod("extractJson", String.class);
        method.setAccessible(true);
        return (String) method.invoke(client, raw);
    }

    /** 反射调用 AgenticClient.parseDeepSeekResponse(String, String) */
    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeParseResponse(String capability, String raw) throws Exception {
        java.lang.reflect.Method method = AgenticClient.class.getDeclaredMethod("parseDeepSeekResponse", String.class, String.class);
        method.setAccessible(true);
        return (Map<String, Object>) method.invoke(client, capability, raw);
    }
}

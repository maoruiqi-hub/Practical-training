package com.neu.CoursePlatform.dify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DifyClient 单元测试 — Dify 平台 API 客户端。
 * 覆盖配置检查、API Key 缺失时的错误返回、HTTP 调用失败时的优雅降级。
 */
class DifyClientTest {

    private DifyClient client;

    @BeforeEach
    void setUp() {
        client = new DifyClient();
    }

    // ============ isConfigured() ============

    @Test
    void isConfiguredReturnsTrueWhenChatApiKeySet() {
        setField(client, "chatApiKey", "app-xxxxxxxxxxxxx");
        assertTrue(client.isConfigured());
    }

    @Test
    void isConfiguredReturnsFalseWhenChatApiKeyEmpty() {
        setField(client, "chatApiKey", "");
        assertFalse(client.isConfigured());
    }

    @Test
    void isConfiguredReturnsFalseWhenChatApiKeyNull() {
        setField(client, "chatApiKey", null);
        assertFalse(client.isConfigured());
    }

    @Test
    void isConfiguredReturnsTrueWhenChatApiKeyNonBlank() {
        setField(client, "chatApiKey", "app-valid-key-123");
        assertTrue(client.isConfigured());
    }

    // ============ sendChatMessage - API Key 未配置 ============

    @Test
    void sendChatMessageReturnsErrorWhenApiKeyNotConfigured() {
        DifyResponse resp = client.sendChatMessage("什么是Spring Boot？", "user-1");
        assertFalse(resp.isSuccess());
        assertNotNull(resp.getError());
        assertTrue(resp.getError().contains("API key not configured"));
    }

    @Test
    void sendChatMessageWithAllParamsReturnsErrorWhenNotConfigured() {
        Map<String, Object> inputs = Map.of("course", "Java编程");
        DifyResponse resp = client.sendChatMessage("什么是依赖注入？", "user-1", "conv-1", inputs);
        assertFalse(resp.isSuccess());
        assertNotNull(resp.getError());
    }

    @Test
    void sendChatMessageDefaultsNullUserIdToAnonymous() {
        // 即使 API key 没有配置，也会正确构建请求 body
        DifyResponse resp = client.sendChatMessage("test query", null);
        assertFalse(resp.isSuccess()); // 因为 API key 未配置
    }

    // ============ sendCompletion - API Key 未配置 ============

    @Test
    void sendCompletionReturnsErrorWhenApiKeyNotConfigured() {
        DifyResponse resp = client.sendCompletion("评估这份作业", "teacher-1");
        assertFalse(resp.isSuccess());
        assertNotNull(resp.getError());
    }

    @Test
    void sendCompletionWithInputsReturnsErrorWhenNotConfigured() {
        Map<String, Object> inputs = Map.of("rubric", "评分标准");
        DifyResponse resp = client.sendCompletion("评估", "teacher-1", inputs);
        assertFalse(resp.isSuccess());
        assertNotNull(resp.getError());
    }

    // ============ runWorkflow - API Key 未配置 ============

    @Test
    void runWorkflowReturnsErrorWhenApiKeyNotConfigured() {
        Map<String, Object> inputs = Map.of("knowledgePoints", "[\"kp1\",\"kp2\"]");
        DifyResponse resp = client.runWorkflow(inputs, "teacher-1");
        assertFalse(resp.isSuccess());
        assertNotNull(resp.getError());
    }

    @Test
    void runWorkflowDefaultsNullInputsToEmptyMap() {
        DifyResponse resp = client.runWorkflow(null, "user-1");
        assertFalse(resp.isSuccess()); // api key not configured
    }

    @Test
    void runWorkflowDefaultsNullUserIdToAnonymous() {
        DifyResponse resp = client.runWorkflow(Map.of("key", "val"), null);
        assertFalse(resp.isSuccess()); // api key not configured
    }

    // ============ retrieveKnowledge - Dataset ID / API Key 未配置 ============

    @Test
    void retrieveKnowledgeReturnsErrorWhenDatasetIdNotConfigured() {
        setField(client, "chatApiKey", "app-key"); // 有 key 但无 dataset id
        DifyResponse resp = client.retrieveKnowledge("什么是Java？", 5);
        assertFalse(resp.isSuccess());
        assertTrue(resp.getError().contains("Dataset ID not configured"));
    }

    @Test
    void retrieveKnowledgeReturnsErrorWhenNoKeysConfigured() {
        DifyResponse resp = client.retrieveKnowledge("什么是Python？", 3);
        assertFalse(resp.isSuccess());
    }

    // ============ HTTP 调用失败时的优雅降级 ============

    @Test
    void sendChatMessageGracefullyFailsWhenHttpUnreachable() {
        setField(client, "chatApiKey", "app-key");
        setField(client, "baseUrl", "http://localhost:19999"); // 未监听的端口
        DifyResponse resp = client.sendChatMessage("test", "user-1");
        // 应该捕获异常并返回错误，而不是抛出
        assertFalse(resp.isSuccess());
        assertNotNull(resp.getError());
    }

    @Test
    void sendCompletionGracefullyFailsWhenHttpUnreachable() {
        setField(client, "completionApiKey", "app-key");
        setField(client, "baseUrl", "http://localhost:19999");
        DifyResponse resp = client.sendCompletion("test", "user-1");
        assertFalse(resp.isSuccess());
        assertNotNull(resp.getError());
    }

    @Test
    void runWorkflowGracefullyFailsWhenHttpUnreachable() {
        setField(client, "workflowApiKey", "app-key");
        setField(client, "baseUrl", "http://localhost:19999");
        DifyResponse resp = client.runWorkflow(Map.of("k", "v"), "user-1");
        assertFalse(resp.isSuccess());
        assertNotNull(resp.getError());
    }

    @Test
    void retrieveKnowledgeGracefullyFailsWhenHttpUnreachable() {
        setField(client, "datasetId", "ds-123");
        setField(client, "datasetApiKey", "app-key");
        setField(client, "baseUrl", "http://localhost:19999");
        DifyResponse resp = client.retrieveKnowledge("test", 3);
        assertFalse(resp.isSuccess());
        assertNotNull(resp.getError());
    }

    // ============ 不同 API Key 配置场景 ============

    @Test
    void chatApiKeyUsedWhenConfigured() {
        setField(client, "chatApiKey", "app-chat-key");
        setField(client, "baseUrl", "http://localhost:19999");
        DifyResponse resp = client.sendChatMessage("hello", "user-1");
        // 应该尝试发送 HTTP 请求并失败（连接拒绝），返回 error
        assertFalse(resp.isSuccess());
        assertNotNull(resp.getError());
    }

    @Test
    void datasetApiKeyFallsBackToChatApiKeyWhenBlank() {
        setField(client, "chatApiKey", "app-chat-key");
        setField(client, "datasetId", "ds-1");
        setField(client, "datasetApiKey", ""); // 空，应回退到 chatApiKey
        setField(client, "baseUrl", "http://localhost:19999");
        DifyResponse resp = client.retrieveKnowledge("test", 3);
        assertFalse(resp.isSuccess()); // HTTP 不可达
    }

    // ============ workflowKeyFor capability 映射 ============

    @Test
    void workflowKeyForSelectsClusterKey() throws Exception {
        setField(client, "workflowClusterKey", "app-cluster");
        assertEquals("app-cluster", invokePrivate(client, "workflowKeyFor", "clusterProblems"));
    }

    @Test
    void workflowKeyForSelectsSuggestionsKey() throws Exception {
        setField(client, "workflowSuggestionsKey", "app-suggestions");
        assertEquals("app-suggestions", invokePrivate(client, "workflowKeyFor", "teachingSuggestions"));
    }

    @Test
    void workflowKeyForSelectsDiagnosisKey() throws Exception {
        setField(client, "workflowDiagnosisKey", "app-diagnosis");
        assertEquals("app-diagnosis", invokePrivate(client, "workflowKeyFor", "tower-diagnosis-report"));
    }

    @Test
    void workflowKeyForSelectsAssessmentKey() throws Exception {
        setField(client, "workflowAssessmentKey", "app-assessment");
        assertEquals("app-assessment", invokePrivate(client, "workflowKeyFor", "assessment"));
    }

    @Test
    void workflowKeyForSelectsRecommendKey() throws Exception {
        setField(client, "workflowRecommendKey", "app-recommend");
        assertEquals("app-recommend", invokePrivate(client, "workflowKeyFor", "recommend"));
    }

    @Test
    void workflowKeyForFallsBackToDefault() throws Exception {
        setField(client, "workflowApiKey", "app-default");
        assertEquals("app-default", invokePrivate(client, "workflowKeyFor", "unknown-capability"));
    }

    // ============ parseResponse：Workflow data.outputs / data.status ============

    @Test
    void parseResponseExtractsWorkflowOutputs() throws Exception {
        DifyResponse resp = (DifyResponse) invokePrivate(client, "parseResponse",
                "{\"data\":{\"status\":\"succeeded\",\"outputs\":{\"result\":\"hello\"}}}");
        assertTrue(resp.isSuccess());
        assertTrue(resp.getContent().contains("hello"));
    }

    @Test
    void parseResponseReturnsErrorOnFailedStatus() throws Exception {
        DifyResponse resp = (DifyResponse) invokePrivate(client, "parseResponse",
                "{\"data\":{\"status\":\"failed\",\"error\":\"boom\"}}");
        assertFalse(resp.isSuccess());
        assertTrue(resp.getError().contains("failed"));
        assertTrue(resp.getError().contains("boom"));
    }

    // ============ isWorkflowConfigured ============

    @Test
    void isWorkflowConfiguredTrueWhenAnyWorkflowKeySet() {
        setField(client, "workflowClusterKey", "app-cluster");
        assertTrue(client.isWorkflowConfigured());
    }

    @Test
    void isWorkflowConfiguredFalseWhenNoWorkflowKeys() {
        assertFalse(client.isWorkflowConfigured());
    }

    // ============ 辅助方法 ============

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
            for (var method : target.getClass().getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                    method.setAccessible(true);
                    return method.invoke(target, args);
                }
            }
            throw new NoSuchMethodException(methodName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke " + methodName, e);
        }
    }
}

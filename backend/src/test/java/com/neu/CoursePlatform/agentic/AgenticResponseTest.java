package com.neu.CoursePlatform.agentic;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgenticResponse 单元测试 — AI 响应 DTO。
 * 验证构造函数、unavailable() 工厂方法以及 @AllArgsConstructor 生成的构造函数。
 */
class AgenticResponseTest {

    // ============ 构造函数 & getter ============

    @Test
    void constructorSetsAllFields() {
        AgenticResponse resp = new AgenticResponse(true,
                Map.of("result", "这是AI生成的回答", "conversationId", "conv-1"),
                "ok");

        assertTrue(resp.isSuccess());
        assertEquals("ok", resp.getMessage());
        assertEquals(Map.of("result", "这是AI生成的回答", "conversationId", "conv-1"), resp.getData());
    }

    @Test
    void constructorWithEmptyData() {
        AgenticResponse resp = new AgenticResponse(false, Map.of(), "error message");
        assertFalse(resp.isSuccess());
        assertEquals("error message", resp.getMessage());
        assertTrue(resp.getData().isEmpty());
    }

    @Test
    void constructorWithNullMessage() {
        AgenticResponse resp = new AgenticResponse(true, Map.of("key", "val"), null);
        assertTrue(resp.isSuccess());
        assertNull(resp.getMessage());
        assertEquals(Map.of("key", "val"), resp.getData());
    }

    // ============ unavailable() ============

    @Test
    void unavailableReturnsFailedResponse() {
        AgenticResponse resp = AgenticResponse.unavailable();
        assertFalse(resp.isSuccess());
        assertEquals("Agentic 服务不可用", resp.getMessage());
        assertNotNull(resp.getData());
        assertTrue(resp.getData().isEmpty());
    }

    @Test
    void unavailableIsConsistentAcrossCalls() {
        AgenticResponse r1 = AgenticResponse.unavailable();
        AgenticResponse r2 = AgenticResponse.unavailable();
        assertEquals(r1.isSuccess(), r2.isSuccess());
        assertEquals(r1.getMessage(), r2.getMessage());
    }

    // ============ 综合场景 ============

    @Test
    void mockModeResponse() {
        AgenticResponse resp = new AgenticResponse(true,
                Map.of("capability", "lecture", "mock", true),
                "Mock response");
        assertTrue(resp.isSuccess());
        assertTrue(resp.getData().containsKey("mock"));
        assertTrue((Boolean) resp.getData().get("mock"));
        assertEquals("Mock response", resp.getMessage());
    }

    @Test
    void aiServiceUnavailableResponse() {
        AgenticResponse resp = AgenticResponse.unavailable();
        assertFalse(resp.isSuccess());
        assertNotNull(resp.getMessage());
        assertTrue(resp.getData().isEmpty());
    }
}

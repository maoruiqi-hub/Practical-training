package com.neu.CoursePlatform.dify;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DifyResponse 单元测试 — 统一响应封装。
 * 覆盖工厂方法、getter/setter、边界条件。
 */
class DifyResponseTest {

    // ============ success() ============

    @Test
    void successSetsSuccessAndContent() {
        DifyResponse resp = DifyResponse.success("hello");
        assertTrue(resp.isSuccess());
        assertEquals("hello", resp.getContent());
        assertNull(resp.getError());
        assertNull(resp.getConversationId());
        assertNull(resp.getMessageId());
    }

    @Test
    void successWithEmptyContent() {
        DifyResponse resp = DifyResponse.success("");
        assertTrue(resp.isSuccess());
        assertEquals("", resp.getContent());
    }

    @Test
    void successWithNullContent() {
        DifyResponse resp = DifyResponse.success(null);
        assertTrue(resp.isSuccess());
        assertNull(resp.getContent());
    }

    @Test
    void successWithJsonContent() {
        DifyResponse resp = DifyResponse.success("{\"key\":\"value\"}");
        assertTrue(resp.isSuccess());
        assertEquals("{\"key\":\"value\"}", resp.getContent());
    }

    // ============ error() ============

    @Test
    void errorSetsErrorAndNotSuccess() {
        DifyResponse resp = DifyResponse.error("something went wrong");
        assertFalse(resp.isSuccess());
        assertEquals("something went wrong", resp.getError());
        assertNull(resp.getContent());
    }

    @Test
    void errorWithEmptyMessage() {
        DifyResponse resp = DifyResponse.error("");
        assertFalse(resp.isSuccess());
        assertEquals("", resp.getError());
    }

    @Test
    void errorWithNullMessage() {
        DifyResponse resp = DifyResponse.error(null);
        assertFalse(resp.isSuccess());
        assertNull(resp.getError());
    }

    // ============ conversationId ============

    @Test
    void conversationIdSettableAndGettable() {
        DifyResponse resp = DifyResponse.success("OK");
        resp.setConversationId("conv-123");
        assertEquals("conv-123", resp.getConversationId());
    }

    @Test
    void conversationIdDefaultNull() {
        DifyResponse resp = DifyResponse.success("OK");
        assertNull(resp.getConversationId());
    }

    @Test
    void conversationIdWithNull() {
        DifyResponse resp = DifyResponse.success("OK");
        resp.setConversationId(null);
        assertNull(resp.getConversationId());
    }

    // ============ messageId ============

    @Test
    void messageIdSettableAndGettable() {
        DifyResponse resp = DifyResponse.success("OK");
        resp.setMessageId("msg-456");
        assertEquals("msg-456", resp.getMessageId());
    }

    @Test
    void messageIdDefaultNull() {
        DifyResponse resp = DifyResponse.success("OK");
        assertNull(resp.getMessageId());
    }

    @Test
    void messageIdWithNull() {
        DifyResponse resp = DifyResponse.success("OK");
        resp.setMessageId(null);
        assertNull(resp.getMessageId());
    }

    // ============ 综合场景 ============

    @Test
    void fullChatResponseRoundTrip() {
        DifyResponse resp = DifyResponse.success("这是一段AI生成的讲解内容");
        resp.setConversationId("conv-chat-001");
        resp.setMessageId("msg-001");
        assertTrue(resp.isSuccess());
        assertEquals("这是一段AI生成的讲解内容", resp.getContent());
        assertEquals("conv-chat-001", resp.getConversationId());
        assertEquals("msg-001", resp.getMessageId());
        assertNull(resp.getError());
    }

    @Test
    void errorResponseHasNoConversationOrMessageId() {
        DifyResponse resp = DifyResponse.error("API key not configured");
        assertFalse(resp.isSuccess());
        assertEquals("API key not configured", resp.getError());
        assertNull(resp.getConversationId());
        assertNull(resp.getMessageId());
    }

    @Test
    void twoSuccessInstancesAreIndependent() {
        DifyResponse r1 = DifyResponse.success("first");
        DifyResponse r2 = DifyResponse.success("second");
        r1.setConversationId("c1");
        r2.setConversationId("c2");

        assertEquals("first", r1.getContent());
        assertEquals("c1", r1.getConversationId());
        assertEquals("second", r2.getContent());
        assertEquals("c2", r2.getConversationId());
    }
}

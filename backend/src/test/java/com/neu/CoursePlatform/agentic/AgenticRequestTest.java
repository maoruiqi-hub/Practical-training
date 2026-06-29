package com.neu.CoursePlatform.agentic;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgenticRequest 单元测试 — AI 请求 DTO。
 * 验证 Lombok @Data 生成的 getter/setter 以及字段默认行为。
 */
class AgenticRequestTest {

    @Test
    void allFieldsSettableAndGettable() {
        AgenticRequest req = new AgenticRequest();
        req.setCourseCode("CS101");
        req.setResourceId("resource-1");
        req.setKnowledgePointId("kp-001");
        req.setContent("请讲解面向对象编程中的多态概念");
        req.setContext(Map.of("userId", "student-1", "pageNumber", 3));

        assertEquals("CS101", req.getCourseCode());
        assertEquals("resource-1", req.getResourceId());
        assertEquals("kp-001", req.getKnowledgePointId());
        assertEquals("请讲解面向对象编程中的多态概念", req.getContent());
        assertEquals(Map.of("userId", "student-1", "pageNumber", 3), req.getContext());
    }

    @Test
    void defaultFieldsAreNull() {
        AgenticRequest req = new AgenticRequest();
        assertNull(req.getCourseCode());
        assertNull(req.getResourceId());
        assertNull(req.getKnowledgePointId());
        assertNull(req.getContent());
        assertNull(req.getContext());
    }

    @Test
    void contentCanBeEmpty() {
        AgenticRequest req = new AgenticRequest();
        req.setContent("");
        assertEquals("", req.getContent());
    }

    @Test
    void contextCanBeEmpty() {
        AgenticRequest req = new AgenticRequest();
        req.setContext(Map.of());
        assertNotNull(req.getContext());
        assertTrue(req.getContext().isEmpty());
    }

    @Test
    void twoInstancesAreIndependent() {
        AgenticRequest r1 = new AgenticRequest();
        r1.setCourseCode("CS101");
        AgenticRequest r2 = new AgenticRequest();
        r2.setCourseCode("CS202");

        assertEquals("CS101", r1.getCourseCode());
        assertEquals("CS202", r2.getCourseCode());
    }
}

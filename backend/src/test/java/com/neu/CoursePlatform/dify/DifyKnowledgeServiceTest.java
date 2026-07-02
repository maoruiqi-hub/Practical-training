package com.neu.CoursePlatform.dify;

import com.neu.CoursePlatform.dify.DifyKnowledgeService.KnowledgeSegment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DifyKnowledgeService 单元测试 — 知识库检索与 RAG Prompt 构建。
 * 使用匿名子类模拟 DifyClient 依赖，覆盖检索、退避、Prompt 构建等逻辑。
 */
class DifyKnowledgeServiceTest {

    // ============ retrieve() — Dify 未配置 ============

    @Test
    void retrieveReturnsEmptyWhenDifyNotConfigured() {
        DifyClient unconfigured = new DifyClient() {
            @Override
            public boolean isConfigured() { return false; }
        };
        DifyKnowledgeService svc = new DifyKnowledgeService(unconfigured);
        List<KnowledgeSegment> result = svc.retrieve("什么是Java", 5);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ============ retrieve() — 检索成功 ============

    @Test
    void retrieveReturnsSegmentsOnSuccess() {
        DifyClient mockClient = difyClientForRetrieve(
                DifyResponse.success("[{\"content\":\"Java是面向对象的编程语言\",\"document_name\":\"Java基础.pdf\",\"score\":0.95}]"));
        DifyKnowledgeService svc = new DifyKnowledgeService(mockClient);

        List<KnowledgeSegment> result = svc.retrieve("Java", 5);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Java是面向对象的编程语言", result.get(0).getContent());
        assertEquals("Java基础.pdf", result.get(0).getDocumentName());
        assertEquals(0.95, result.get(0).getScore(), 0.001);
    }

    @Test
    void retrieveHandlesMultipleSegments() {
        DifyClient mockClient = difyClientForRetrieve(
                DifyResponse.success("""
                [{"content":"Python基础语法","document_name":"Python入门.pdf","score":0.98},
                 {"content":"Python面向对象","document_name":"Python进阶.pdf","score":0.87}]
                """));
        DifyKnowledgeService svc = new DifyKnowledgeService(mockClient);

        List<KnowledgeSegment> result = svc.retrieve("Python", 5);
        assertEquals(2, result.size());
        assertEquals("Python基础语法", result.get(0).getContent());
        assertEquals("Python面向对象", result.get(1).getContent());
    }

    @Test
    void retrieveHandlesEmptyJsonArray() {
        DifyClient mockClient = difyClientForRetrieve(DifyResponse.success("[]"));
        DifyKnowledgeService svc = new DifyKnowledgeService(mockClient);

        List<KnowledgeSegment> result = svc.retrieve("unknown topic", 3);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ============ retrieve() — 检索失败 & 退避 ============

    @Test
    void retrieveReturnsEmptyOnFailureAndTriggersBackoff() {
        DifyClient mockClient = difyClientForRetrieve(
                DifyResponse.error("Knowledge base not found"));
        DifyKnowledgeService svc = new DifyKnowledgeService(mockClient);

        // 第一次检索失败，触发退避
        List<KnowledgeSegment> result1 = svc.retrieve("test", 3);
        assertNotNull(result1);
        assertTrue(result1.isEmpty(), "应返回空列表");

        // 在退避期内的第二次调用，因为退避直接返回空列表
        List<KnowledgeSegment> result2 = svc.retrieve("test", 3);
        assertNotNull(result2);
        assertTrue(result2.isEmpty(), "退避期内仍返回空列表");
    }

    // ============ retrieve() — 解析失败容错 ============

    @Test
    void retrieveHandlesMalformedJson() {
        DifyClient mockClient = difyClientForRetrieve(DifyResponse.success("not valid json"));
        DifyKnowledgeService svc = new DifyKnowledgeService(mockClient);

        List<KnowledgeSegment> result = svc.retrieve("test", 3);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void retrieveHandlesSegmentMissingOptionalFields() {
        DifyClient mockClient = difyClientForRetrieve(
                DifyResponse.success("[{\"content\":\"仅包含内容的记录\"}]"));
        DifyKnowledgeService svc = new DifyKnowledgeService(mockClient);

        List<KnowledgeSegment> result = svc.retrieve("test", 3);
        assertEquals(1, result.size());
        assertEquals("仅包含内容的记录", result.get(0).getContent());
        assertNull(result.get(0).getDocumentName());
        assertEquals(0.0, result.get(0).getScore(), 0.001);
    }

    // ============ buildRagPrompt() ============

    @Test
    void buildRagPromptReturnsOriginalQueryWhenNoSegments() {
        DifyClient unconfigured = new DifyClient() {
            @Override
            public boolean isConfigured() { return false; }
        };
        DifyKnowledgeService svc = new DifyKnowledgeService(unconfigured);

        String prompt = svc.buildRagPrompt("什么是多态？", "CS101", "kp-polymorphism");
        assertEquals("什么是多态？", prompt);
    }

    @Test
    void buildRagPromptIncludesRetrievedContext() {
        DifyClient mockClient = difyClientForRetrieve(
                DifyResponse.success("[{\"content\":\"多态是面向对象的三大特性之一\",\"document_name\":\"面向对象编程.pdf\",\"score\":0.95}]"));
        DifyKnowledgeService svc = new DifyKnowledgeService(mockClient);

        String prompt = svc.buildRagPrompt("什么是多态？", "CS101", "kp-polymorphism");

        assertNotNull(prompt);
        assertTrue(prompt.contains("【参考课程资料】"));
        assertTrue(prompt.contains("多态是面向对象的三大特性之一"));
        assertTrue(prompt.contains("（来源：面向对象编程.pdf）"));
        assertTrue(prompt.contains("【用户问题】"));
        assertTrue(prompt.contains("什么是多态？"));
        assertTrue(prompt.contains("请根据以上课程资料回答用户问题"));
    }

    @Test
    void buildRagPromptWithMultipleSegments() {
        DifyClient mockClient = difyClientForRetrieve(
                DifyResponse.success("""
                [{"content":"片段A","document_name":"docA.pdf","score":0.99},
                 {"content":"片段B","document_name":"docB.pdf","score":0.80}]
                """));
        DifyKnowledgeService svc = new DifyKnowledgeService(mockClient);

        String prompt = svc.buildRagPrompt("问题", "CS101", "kp-1");
        assertTrue(prompt.contains("资料1：片段A"));
        assertTrue(prompt.contains("资料2：片段B"));
    }

    @Test
    void buildRagPromptWithoutDocumentNameDoesNotAppendSource() {
        DifyClient mockClient = difyClientForRetrieve(
                DifyResponse.success("[{\"content\":\"匿名内容片段\",\"score\":0.88}]"));
        DifyKnowledgeService svc = new DifyKnowledgeService(mockClient);

        String prompt = svc.buildRagPrompt("问题", "CS101", null);
        assertTrue(prompt.contains("匿名内容片段"));
        assertFalse(prompt.contains("（来源："));
    }

    @Test
    void buildRagPromptWithNonIntegerScore() {
        DifyClient mockClient = difyClientForRetrieve(
                DifyResponse.success("[{\"content\":\"C\",\"score\":0.75}]"));
        DifyKnowledgeService svc = new DifyKnowledgeService(mockClient);

        String prompt = svc.buildRagPrompt("Q", "CS101", null);
        assertTrue(prompt.contains("资料1：C"));
        assertFalse(prompt.contains("（来源："));
    }

    // ============ KnowledgeSegment POJO ============

    @Test
    void knowledgeSegmentDefaultValues() {
        KnowledgeSegment seg = new KnowledgeSegment();
        assertNull(seg.getContent());
        assertNull(seg.getDocumentName());
        assertEquals(0.0, seg.getScore(), 0.001);
    }

    @Test
    void knowledgeSegmentFullRoundTrip() {
        KnowledgeSegment seg = new KnowledgeSegment();
        seg.setContent("测试内容");
        seg.setDocumentName("测试文档.pdf");
        seg.setScore(0.92);

        assertEquals("测试内容", seg.getContent());
        assertEquals("测试文档.pdf", seg.getDocumentName());
        assertEquals(0.92, seg.getScore(), 0.001);
    }

    // ============ helper ============

    /** 创建模拟 DifyClient，对 retrieveKnowledge 返回指定响应 */
    private static DifyClient difyClientForRetrieve(DifyResponse response) {
        return new DifyClient() {
            @Override
            public boolean isConfigured() { return true; }

            @Override
            public DifyResponse retrieveKnowledge(String query, int topK) {
                return response;
            }
        };
    }
}

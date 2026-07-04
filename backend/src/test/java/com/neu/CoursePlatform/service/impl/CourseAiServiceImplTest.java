package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.agentic.AgenticRequest;
import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dify.DifyClient;
import com.neu.CoursePlatform.dify.DifyKnowledgeService;
import com.neu.CoursePlatform.dto.AbilityMapDTO;
import com.neu.CoursePlatform.dto.CourseQaRequest;
import com.neu.CoursePlatform.dto.LectureRequest;
import com.neu.CoursePlatform.entity.CourseResource;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.service.CourseResourceService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.AbilityPointService;
import com.neu.CoursePlatform.service.AbilityMapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CourseAiServiceImpl 单元测试 — AI 课程服务。
 * 覆盖知识点讲解、问答、能力图谱生成三大功能。
 * 使用动态代理模拟接口类依赖（KnowledgePointService、CourseResourceService），
 * 使用匿名子类模拟具体类依赖（AgenticClient）。
 */
class CourseAiServiceImplTest {

    private CourseAiServiceImpl service;
    private KnowledgePointService knowledgePointService;
    private CourseResourceService courseResourceService;

    // ============ explainKnowledgePoint() ============

    @Test
    void explainKnowledgePointSuccess() {
        setupWithMockAi(true);
        LectureRequest req = lectureRequest("resource-1", 1, "请讲解一下");

        Result<AgenticResponse> result = service.explainKnowledgePoint("kp-1", req);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().isSuccess());
        assertEquals("Mock AI response", result.getData().getData().get("answer"));
    }

    @Test
    void explainKnowledgePointFailsWhenKpNotFound() {
        setupWithMockAi(true);
        LectureRequest req = lectureRequest("resource-1", 1, "请讲解");

        Result<AgenticResponse> result = service.explainKnowledgePoint("kp-nonexistent", req);
        assertNotEquals(200, result.getCode());
        assertEquals("知识点不存在", result.getMsg());
    }

    @Test
    void explainKnowledgePointFailsWhenRequestNull() {
        setupWithMockAi(true);
        Result<AgenticResponse> result = service.explainKnowledgePoint("kp-1", null);
        assertNotEquals(200, result.getCode());
        assertEquals("请选择 PPT 资源", result.getMsg());
    }

    @Test
    void explainKnowledgePointFailsWhenResourceIdNull() {
        setupWithMockAi(true);
        LectureRequest req = new LectureRequest();
        req.setResourceId(null);
        req.setPageNumber(1);
        req.setQuestion("请讲解");

        Result<AgenticResponse> result = service.explainKnowledgePoint("kp-1", req);
        assertNotEquals(200, result.getCode());
        assertEquals("请选择 PPT 资源", result.getMsg());
    }

    @Test
    void explainKnowledgePointFailsWhenResourceNotFound() {
        setupWithMockAi(true);
        LectureRequest req = lectureRequest("resource-nonexistent", 1, "请讲解");

        Result<AgenticResponse> result = service.explainKnowledgePoint("kp-1", req);
        assertNotEquals(200, result.getCode());
        assertEquals("PPT 资源不属于该知识点课程", result.getMsg());
    }

    @Test
    void explainKnowledgePointFailsWhenResourceNotPpt() {
        setupWithMockAi(true);
        LectureRequest req = lectureRequest("video-1", 1, "请讲解");

        Result<AgenticResponse> result = service.explainKnowledgePoint("kp-1", req);
        assertNotEquals(200, result.getCode());
    }

    @Test
    void explainKnowledgePointFailsWhenResourceCourseMismatch() {
        setupWithMockAi(true);
        LectureRequest req = lectureRequest("resource-cs202", 1, "请讲解");

        Result<AgenticResponse> result = service.explainKnowledgePoint("kp-1", req);
        assertNotEquals(200, result.getCode());
        assertEquals("PPT 资源不属于该知识点课程", result.getMsg());
    }

    @Test
    void explainKnowledgePointUsesLocalFallbackWhenAiUnavailable() {
        setupWithMockAi(false); // AI 返回失败
        LectureRequest req = lectureRequest("resource-1", 1, "请讲解");

        Result<AgenticResponse> result = service.explainKnowledgePoint("kp-1", req);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().isSuccess());
        assertEquals("local_fallback", result.getData().getData().get("source"));
        assertNotNull(result.getData().getData().get("answer"));
    }

    @Test
    void explainKnowledgePointHandlesPageNumberNull() {
        setupWithMockAi(true);
        LectureRequest req = lectureRequest("resource-1", null, "请讲解");

        Result<AgenticResponse> result = service.explainKnowledgePoint("kp-1", req);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    // ============ answerKnowledgePointQuestion() ============

    @Test
    void answerKnowledgePointQuestionSuccess() {
        setupWithMockAi(true);
        CourseQaRequest req = qaRequest("什么是Java多态？", null);

        Result<AgenticResponse> result = service.answerKnowledgePointQuestion("kp-1", req);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().isSuccess());
        assertEquals("Mock AI response", result.getData().getData().get("answer"));
    }

    @Test
    void answerKnowledgePointQuestionFailsWhenKpNotFound() {
        setupWithMockAi(true);
        CourseQaRequest req = qaRequest("什么是多态？", null);

        Result<AgenticResponse> result = service.answerKnowledgePointQuestion("kp-nonexistent", req);
        assertNotEquals(200, result.getCode());
        assertEquals("知识点不存在", result.getMsg());
    }

    @Test
    void answerKnowledgePointQuestionFailsWhenQuestionNull() {
        setupWithMockAi(true);
        Result<AgenticResponse> result = service.answerKnowledgePointQuestion("kp-1", null);
        assertNotEquals(200, result.getCode());
        assertEquals("问题不能为空", result.getMsg());
    }

    @Test
    void answerKnowledgePointQuestionFailsWhenQuestionBlank() {
        setupWithMockAi(true);
        CourseQaRequest req = qaRequest("  ", null);

        Result<AgenticResponse> result = service.answerKnowledgePointQuestion("kp-1", req);
        assertNotEquals(200, result.getCode());
        assertEquals("问题不能为空", result.getMsg());
    }

    @Test
    void answerKnowledgePointQuestionFailsWhenQuestionEmpty() {
        setupWithMockAi(true);
        CourseQaRequest req = qaRequest("", null);

        Result<AgenticResponse> result = service.answerKnowledgePointQuestion("kp-1", req);
        assertNotEquals(200, result.getCode());
        assertEquals("问题不能为空", result.getMsg());
    }

    @Test
    void answerKnowledgePointQuestionWithValidResource() {
        setupWithMockAi(true);
        CourseQaRequest req = qaRequest("请解释这个PPT的概念", "resource-1");

        Result<AgenticResponse> result = service.answerKnowledgePointQuestion("kp-1", req);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    void answerKnowledgePointQuestionFailsWhenResourceNotFound() {
        setupWithMockAi(true);
        CourseQaRequest req = qaRequest("请解释", "resource-nonexistent");

        Result<AgenticResponse> result = service.answerKnowledgePointQuestion("kp-1", req);
        assertNotEquals(200, result.getCode());
        assertEquals("课程资料不属于当前课程", result.getMsg());
    }

    @Test
    void answerKnowledgePointQuestionFailsWhenResourceCourseMismatch() {
        setupWithMockAi(true);
        CourseQaRequest req = qaRequest("请解释", "resource-cs202");

        Result<AgenticResponse> result = service.answerKnowledgePointQuestion("kp-1", req);
        assertNotEquals(200, result.getCode());
        assertEquals("课程资料不属于当前课程", result.getMsg());
    }

    @Test
    void answerKnowledgePointQuestionUsesLocalFallbackWhenAiUnavailable() {
        setupWithMockAi(false); // AI 返回失败
        CourseQaRequest req = qaRequest("什么是Java？", null);

        Result<AgenticResponse> result = service.answerKnowledgePointQuestion("kp-1", req);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().isSuccess());
        assertEquals("local_fallback", result.getData().getData().get("source"));
        assertNotNull(result.getData().getData().get("answer"));
    }

    // ============ generateAbilityMap() ============

    @Test
    void generateAbilityMapSuccess() {
        setupWithMockAi(true);
        Result<AgenticResponse> result = service.generateAbilityMap("CS101");
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().isSuccess());
    }

    @Test
    void generateAbilityMapReturnsFallbackWhenAiUnavailable() {
        setupWithMockAi(false);
        Result<AgenticResponse> result = service.generateAbilityMap("CS101");
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().isSuccess());
        assertEquals("local_fallback", result.getData().getData().get("source"));
        assertEquals(true, result.getData().getData().get("fallback"));
        assertNotNull(result.getData().getData().get("abilityPoints"));
    }

    @Test
    void generateAbilityMapWithEmptyKnowledgePoints() {
        // 设置空的 knowledge point 列表
        KnowledgePointService emptyKpService = createKnowledgePointService(List.of());
        AgenticClient aiClient = createMockAgenticClient(true);
        CourseResourceService resService = createCourseResourceService();
        service = new CourseAiServiceImpl(aiClient, emptyKpService, resService,
                createAbilityPointService(), createAbilityMapService());

        Result<AgenticResponse> result = service.generateAbilityMap("CS999");
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    // ============ 综合场景 ============

    @Test
    void explainKnowledgePointWithPreviousMessages() {
        setupWithMockAi(true);
        LectureRequest req = lectureRequest("resource-1", 2, "继续讲解");
        req.setPreviousMessages(List.of("之前的问题", "之前的回答"));

        Result<AgenticResponse> result = service.explainKnowledgePoint("kp-1", req);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    void answerKnowledgePointQuestionWithPreviousMessages() {
        setupWithMockAi(true);
        CourseQaRequest req = qaRequest("追问一下", "resource-1");
        req.setPreviousMessages(List.of("Q: 什么是继承？", "A: 继承是..."));

        Result<AgenticResponse> result = service.answerKnowledgePointQuestion("kp-1", req);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    void answerKnowledgePointQuestionWithoutResourceIdUsesFallbackTitle() {
        setupWithMockAi(true);
        CourseQaRequest req = qaRequest("如何学习Java？", null);

        Result<AgenticResponse> result = service.answerKnowledgePointQuestion("kp-1", req);
        assertEquals(200, result.getCode());
    }

    // ============ helper: 构建测试环境 ============

    private void setupWithMockAi(boolean aiAvailable) {
        knowledgePointService = createKnowledgePointService(List.of(
                kp("kp-1", "CS101", "Java多态"),
                kp("kp-2", "CS101", "Java继承"),
                kp("kp-3", "CS202", "Python基础")
        ));
        courseResourceService = createCourseResourceService();
        AgenticClient aiClient = createMockAgenticClient(aiAvailable);
        service = new CourseAiServiceImpl(aiClient, knowledgePointService, courseResourceService,
                createAbilityPointService(), createAbilityMapService());
    }

    // ============ 模拟依赖 ============

    private static KnowledgePointService createKnowledgePointService(List<KnowledgePoint> points) {
        return (KnowledgePointService) Proxy.newProxyInstance(
                KnowledgePointService.class.getClassLoader(),
                new Class<?>[]{KnowledgePointService.class},
                (proxy, method, args) -> {
                    if ("getById".equals(method.getName())) {
                        String id = (String) args[0];
                        return points.stream()
                                .filter(kp -> id.equals(kp.getKnowledgePointId()))
                                .findFirst().orElse(null);
                    }
                    if ("listByCourseCode".equals(method.getName())) {
                        String courseCode = (String) args[0];
                        return points.stream()
                                .filter(kp -> courseCode.equals(kp.getCourseCode()))
                                .toList();
                    }
                    return null;
                });
    }

    private static CourseResourceService createCourseResourceService() {
        return (CourseResourceService) Proxy.newProxyInstance(
                CourseResourceService.class.getClassLoader(),
                new Class<?>[]{CourseResourceService.class},
                (proxy, method, args) -> {
                    if ("getById".equals(method.getName())) {
                        String id = (String) args[0];
                        return switch (id) {
                            case "resource-1" -> resource("resource-1", "CS101", "Java基础.pptx", "ppt");
                            case "resource-2" -> resource("resource-2", "CS101", "课程视频.mp4", "video");
                            case "resource-cs202" -> resource("resource-cs202", "CS202", "Python.pptx", "ppt");
                            case "video-1" -> resource("video-1", "CS101", "Java视频", "video");
                            default -> null;
                        };
                    }
                    return null;
                });
    }

    private static AbilityPointService createAbilityPointService() {
        return (AbilityPointService) Proxy.newProxyInstance(
                AbilityPointService.class.getClassLoader(),
                new Class<?>[]{AbilityPointService.class},
                (proxy, method, args) -> args != null && args.length > 0 && "getById".equals(method.getName())
                        ? new com.neu.CoursePlatform.entity.AbilityPoint() : List.of());
    }

    private static AbilityMapService createAbilityMapService() {
        return (AbilityMapService) Proxy.newProxyInstance(
                AbilityMapService.class.getClassLoader(),
                new Class<?>[]{AbilityMapService.class},
                (proxy, method, args) -> {
                    if ("getByCourseCode".equals(method.getName())) {
                        return new AbilityMapDTO(List.of(), List.of()); // 空数据 — 触发 Level 2 兜底
                    }
                    return List.of();
                });
    }

    /** 使用匿名子类模拟 AgenticClient（具体类，不是接口） */
    private static AgenticClient createMockAgenticClient(boolean success) {
        // 创建一个不做任何事的 DifyClient 和 DifyKnowledgeService 用于父类构造
        DifyClient dummyClient = new DifyClient() {
            @Override
            public boolean isConfigured() { return false; }
        };
        DifyKnowledgeService dummyKnowledge = new DifyKnowledgeService(dummyClient);

        return new AgenticClient(dummyClient, dummyKnowledge) {
            @Override
            public AgenticResponse invoke(String capability, AgenticRequest request) {
                if (success) {
                    return new AgenticResponse(true,
                            Map.of("result", "Mock AI response", "capability", capability),
                            "ok");
                } else {
                    return AgenticResponse.unavailable();
                }
            }
        };
    }

    // ============ 构造测试数据 ============

    private static KnowledgePoint kp(String id, String courseCode, String name) {
        KnowledgePoint kp = new KnowledgePoint();
        kp.setKnowledgePointId(id);
        kp.setCourseCode(courseCode);
        kp.setName(name);
        return kp;
    }

    private static CourseResource resource(String id, String courseCode, String title, String type) {
        CourseResource r = new CourseResource();
        r.setResourceId(id);
        r.setCourseCode(courseCode);
        r.setTitle(title);
        r.setResourceType(type);
        return r;
    }

    private static LectureRequest lectureRequest(String resourceId, Integer pageNumber, String question) {
        LectureRequest req = new LectureRequest();
        req.setResourceId(resourceId);
        req.setPageNumber(pageNumber);
        req.setQuestion(question);
        return req;
    }

    private static CourseQaRequest qaRequest(String question, String resourceId) {
        CourseQaRequest req = new CourseQaRequest();
        req.setQuestion(question);
        req.setResourceId(resourceId);
        return req;
    }
}

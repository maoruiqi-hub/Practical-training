package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.agentic.AgenticRequest;
import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dify.DifyClient;
import com.neu.CoursePlatform.dify.DifyKnowledgeService;
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
    void explainKnowledgePointReturns503WhenAiUnavailable() {
        setupWithMockAi(false); // AI 返回失败
        LectureRequest req = lectureRequest("resource-1", 1, "请讲解");

        Result<AgenticResponse> result = service.explainKnowledgePoint("kp-1", req);
        assertEquals(503, result.getCode());
        assertEquals("AI 服务暂不可用", result.getMsg());
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
    void answerKnowledgePointQuestionReturns503WhenAiUnavailable() {
        setupWithMockAi(false); // AI 返回失败
        CourseQaRequest req = qaRequest("什么是Java？", null);

        Result<AgenticResponse> result = service.answerKnowledgePointQuestion("kp-1", req);
        assertEquals(503, result.getCode());
        assertEquals("AI 服务暂不可用", result.getMsg());
    }

    // ============ generateAbilityMap() ============

    @Test
    void generateAbilityMapSuccess() {
        setupWithMockAi(true);
        Result<AgenticResponse> result = service.generateAbilityMap("CS101");
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().isSuccess());
        assertNotNull(result.getData().getData().get("saved"));
    }

    @Test
    void generateAbilityMapReturns503WhenAiUnavailable() {
        setupWithMockAi(false);
        Result<AgenticResponse> result = service.generateAbilityMap("CS101");
        assertEquals(503, result.getCode());
        assertEquals("AI 服务暂不可用", result.getMsg());
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
        assertNotEquals(200, result.getCode());
        assertEquals("请先为课程生成或维护知识点", result.getMsg());
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

    // ============ 边界条件测试 ============

    @Test
    void answerKnowledgePointQuestionWithBlankResourceId() {
        setupWithMockAi(true);
        CourseQaRequest req = qaRequest("请解释", "  ");

        Result<AgenticResponse> result = service.answerKnowledgePointQuestion("kp-1", req);
        assertEquals(200, result.getCode());
    }

    @Test
    void explainKnowledgePointWithResourceHavingNullType() {
        setupWithMockAiForNullResourceType();
        LectureRequest req = lectureRequest("resource-null-type", 1, "请讲解");

        Result<AgenticResponse> result = service.explainKnowledgePoint("kp-1", req);
        assertNotEquals(200, result.getCode());
        assertEquals("PPT 资源不属于该知识点课程", result.getMsg());
    }

    @Test
    void generateAbilityMapWithAIResponseNodesFormat() {
        setupWithMockAiForNodesResponse();
        Result<AgenticResponse> result = service.generateAbilityMap("CS101");
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().isSuccess());
    }

    @Test
    void generateAbilityMapThrowsWhenAiReturnsNoCandidates() {
        knowledgePointService = createKnowledgePointService(List.of(kp("kp-1", "CS101", "Java多态")));
        service = new CourseAiServiceImpl(createEmptyAbilityMapAgenticClient(), knowledgePointService,
                createCourseResourceService(), createAbilityPointService(), createAbilityMapService());

        assertThrows(IllegalArgumentException.class, () -> service.generateAbilityMap("CS101"));
    }

    @Test
    void generateAbilityMapReusesExistingAbilityAndResolvesKnowledgePointMaps() {
        knowledgePointService = createKnowledgePointService(List.of(
                kp("kp-1", "CS101", "Java多态"),
                kp("kp-2", "CS101", "Java继承")
        ));
        var existingAbility = new com.neu.CoursePlatform.entity.AbilityPoint();
        existingAbility.setAbilityPointId("ap-1");
        existingAbility.setName("Java能力");
        existingAbility.setCourseCode("CS101");
        AbilityPointService abilityPointService = (AbilityPointService) Proxy.newProxyInstance(
                AbilityPointService.class.getClassLoader(),
                new Class<?>[]{AbilityPointService.class},
                (proxy, method, args) -> {
                    if ("listByCourseCode".equals(method.getName())) return List.of(existingAbility);
                    if ("save".equals(method.getName())) return true;
                    return List.of();
                });

        service = new CourseAiServiceImpl(createAliasAbilityMapAgenticClient(), knowledgePointService,
                createCourseResourceService(), abilityPointService, createAbilityMapService());

        Result<AgenticResponse> result = service.generateAbilityMap("CS101");

        assertEquals(200, result.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> saved = (Map<String, Object>) result.getData().getData().get("saved");
        assertEquals(1, saved.get("reusedAbilityPoints"));
    }

    // ============ normalizeChatResponse 间接测试 ============

    @Test
    void explainKnowledgePointWithAiResponseContainingContentInsteadOfAnswer() {
        setupWithMockAi(true); // AI response has "result" which gets normalized to "answer"
        LectureRequest req = lectureRequest("resource-1", 1, "测试");

        Result<AgenticResponse> result = service.explainKnowledgePoint("kp-1", req);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData().getData().get("answer"));
    }

    @Test
    void answerKnowledgePointQuestionWithAiResponseContainingContentField() {
        setupWithMockAiWithContentField();
        CourseQaRequest req = qaRequest("测试问题", null);

        Result<AgenticResponse> result = service.answerKnowledgePointQuestion("kp-1", req);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData().getData().get("answer"));
    }

    @Test
    void explainKnowledgePointHandlesPreviousMessagesEmptyList() {
        setupWithMockAi(true);
        LectureRequest req = lectureRequest("resource-1", 1, "请讲解");
        req.setPreviousMessages(List.of());

        Result<AgenticResponse> result = service.explainKnowledgePoint("kp-1", req);
        assertEquals(200, result.getCode());
    }

    @Test
    void privateAbilityMapFallbacksCoverNullsAliasesAndEmptyNodes() throws Exception {
        KnowledgePoint nullId = kp(null, "CS101", "无编号");
        KnowledgePoint blankName = kp("kp-blank", "CS101", " ");
        KnowledgePoint named = kp("kp-2", "CS101", "Java继承");
        var existingWithoutName = new com.neu.CoursePlatform.entity.AbilityPoint();
        existingWithoutName.setAbilityPointId("ap-null");
        AbilityPointService abilityPointService = (AbilityPointService) Proxy.newProxyInstance(
                AbilityPointService.class.getClassLoader(),
                new Class<?>[]{AbilityPointService.class},
                (proxy, method, args) -> {
                    if ("listByCourseCode".equals(method.getName())) return List.of(existingWithoutName);
                    if ("save".equals(method.getName())) {
                        ((com.neu.CoursePlatform.entity.AbilityPoint) args[0]).setAbilityPointId("ap-new");
                        return true;
                    }
                    return List.of();
                });
        service = new CourseAiServiceImpl(createEmptyAbilityMapAgenticClient(),
                createKnowledgePointService(List.of(nullId, blankName, named)),
                createCourseResourceService(), abilityPointService, createAbilityMapService());

        @SuppressWarnings("unchecked")
        Map<String, Object> saved = (Map<String, Object>) invokePrivate("saveGeneratedAbilityMap",
                new Class<?>[]{String.class, List.class, Map.class},
                "CS101", List.of(nullId, blankName, named), Map.of("abilities", List.of(
                        Map.of("name", " "),
                        Map.of("label", "新能力", "desc", "描述", "kpIds", "Java继承")
                )));

        assertEquals(1, saved.get("createdAbilityPoints"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> emptyCandidates = (List<Map<String, Object>>) invokePrivate("extractAbilityCandidates",
                new Class<?>[]{Map.class, List.class},
                Map.of("nodes", List.of(Map.of("id", "missing"), Map.of("id", "kp-2", "name", " "))),
                List.of(named));
        assertTrue(emptyCandidates.isEmpty());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> noCandidates = (List<Map<String, Object>>) invokePrivate("extractAbilityCandidates",
                new Class<?>[]{Map.class, List.class}, Map.of("unknown", "value"), List.of(named));
        assertTrue(noCandidates.isEmpty());
    }

    @Test
    void privateNormalizeChatResponseUsesMessageFallbackAndStatusDetection() throws Exception {
        setupWithMockAi(true);

        AgenticResponse messageFallback = (AgenticResponse) invokePrivate("normalizeChatResponse",
                new Class<?>[]{AgenticResponse.class},
                new AgenticResponse(true, Map.of(), "这里是答案"));
        assertEquals("这里是答案", messageFallback.getData().get("answer"));

        AgenticResponse successMessage = (AgenticResponse) invokePrivate("normalizeChatResponse",
                new Class<?>[]{AgenticResponse.class},
                new AgenticResponse(true, null, "success"));
        assertFalse(successMessage.getData().containsKey("answer"));

        assertNull(invokePrivate("normalizeChatResponse", new Class<?>[]{AgenticResponse.class}, new Object[]{null}));
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
                (proxy, method, args) -> {
                    if ("getById".equals(method.getName())) return new com.neu.CoursePlatform.entity.AbilityPoint();
                    if ("save".equals(method.getName())) return true;
                    if ("listByCourseCode".equals(method.getName())) return List.of();
                    return List.of();
                });
    }

    private static AbilityMapService createAbilityMapService() {
        return (AbilityMapService) Proxy.newProxyInstance(
                AbilityMapService.class.getClassLoader(),
                new Class<?>[]{AbilityMapService.class},
                (proxy, method, args) -> {
                    if ("bindKnowledgePoint".equals(method.getName())) return true;
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
                    if ("ability-map".equals(capability)) {
                        return new AgenticResponse(true,
                                Map.of("abilityPoints",
                                        List.of(Map.of("name", "Java基础能力", "description", "Java基础",
                                                "knowledgePointIds", List.of("kp-1", "kp-2")))),
                                "ok");
                    }
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

    // ============ 额外 helper: 支持边界条件测试 ============

    /** CourseResourceService 会返回 resourceType 为 null 的资源 */
    private void setupWithMockAiForNullResourceType() {
        knowledgePointService = createKnowledgePointService(List.of(
                kp("kp-1", "CS101", "Java多态")
        ));
        AgenticClient aiClient = createMockAgenticClient(true);
        CourseResourceService resService = createCourseResourceServiceWithNullType();
        service = new CourseAiServiceImpl(aiClient, knowledgePointService, resService,
                createAbilityPointService(), createAbilityMapService());
    }

    /** AgenticClient 返回 nodes 格式的能力图谱 */
    private void setupWithMockAiForNodesResponse() {
        knowledgePointService = createKnowledgePointService(List.of(
                kp("kp-1", "CS101", "Java多态"),
                kp("kp-2", "CS101", "Java继承")
        ));
        AgenticClient aiClient = createNodesResponseAgenticClient();
        service = new CourseAiServiceImpl(aiClient, knowledgePointService, createCourseResourceService(),
                createAbilityPointService(), createAbilityMapService());
    }

    /** AgenticClient 在 data 中使用 "content" 字段而非 "answer" */
    private void setupWithMockAiWithContentField() {
        knowledgePointService = createKnowledgePointService(List.of(
                kp("kp-1", "CS101", "Java多态")
        ));
        courseResourceService = createCourseResourceService();
        AgenticClient aiClient = createContentFieldAgenticClient();
        service = new CourseAiServiceImpl(aiClient, knowledgePointService, courseResourceService,
                createAbilityPointService(), createAbilityMapService());
    }

    private static AgenticClient createNodesResponseAgenticClient() {
        DifyClient dummyClient = new DifyClient() {
            @Override
            public boolean isConfigured() { return false; }
        };
        DifyKnowledgeService dummyKnowledge = new DifyKnowledgeService(dummyClient);
        return new AgenticClient(dummyClient, dummyKnowledge) {
            @Override
            public AgenticResponse invoke(String capability, AgenticRequest request) {
                return new AgenticResponse(true,
                        Map.of("nodes",
                                List.of(Map.of("id", "kp-1", "name", "Java基础能力"),
                                        Map.of("id", "kp-2", "name", "Java高级能力"))),
                        "ok");
            }
        };
    }

    private static AgenticClient createEmptyAbilityMapAgenticClient() {
        DifyClient dummyClient = new DifyClient() {
            @Override
            public boolean isConfigured() { return false; }
        };
        DifyKnowledgeService dummyKnowledge = new DifyKnowledgeService(dummyClient);
        return new AgenticClient(dummyClient, dummyKnowledge) {
            @Override
            public AgenticResponse invoke(String capability, AgenticRequest request) {
                return new AgenticResponse(true, Map.of("abilityPoints", List.of()), "ok");
            }
        };
    }

    private static AgenticClient createAliasAbilityMapAgenticClient() {
        DifyClient dummyClient = new DifyClient() {
            @Override
            public boolean isConfigured() { return false; }
        };
        DifyKnowledgeService dummyKnowledge = new DifyKnowledgeService(dummyClient);
        return new AgenticClient(dummyClient, dummyKnowledge) {
            @Override
            public AgenticResponse invoke(String capability, AgenticRequest request) {
                return new AgenticResponse(true,
                        Map.of("items", List.of(Map.of(
                                "title", "Java能力",
                                "summary", "复用已有能力点",
                                "knowledge_points", List.of(
                                        Map.of("id", "kp-1"),
                                        Map.of("name", "Java继承")
                                )))),
                        "ok");
            }
        };
    }

    private static AgenticClient createContentFieldAgenticClient() {
        DifyClient dummyClient = new DifyClient() {
            @Override
            public boolean isConfigured() { return false; }
        };
        DifyKnowledgeService dummyKnowledge = new DifyKnowledgeService(dummyClient);
        return new AgenticClient(dummyClient, dummyKnowledge) {
            @Override
            public AgenticResponse invoke(String capability, AgenticRequest request) {
                return new AgenticResponse(true,
                        Map.of("content", "这是通过content字段返回的答案"),
                        "ok");
            }
        };
    }

    private static CourseResourceService createCourseResourceServiceWithNullType() {
        return (CourseResourceService) Proxy.newProxyInstance(
                CourseResourceService.class.getClassLoader(),
                new Class<?>[]{CourseResourceService.class},
                (proxy, method, args) -> {
                    if ("getById".equals(method.getName())) {
                        String id = (String) args[0];
                        if ("resource-null-type".equals(id)) {
                            return resource("resource-null-type", "CS101", "未知类型.pptx", null);
                        }
                        return switch (id) {
                            case "resource-1" -> resource("resource-1", "CS101", "Java基础.pptx", "ppt");
                            default -> null;
                        };
                    }
                    return null;
                });
    }

    private Object invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        java.lang.reflect.Method method = CourseAiServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(service, args);
    }
}

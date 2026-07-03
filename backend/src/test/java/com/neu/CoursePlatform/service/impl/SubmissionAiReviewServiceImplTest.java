package com.neu.CoursePlatform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.agentic.AgenticRequest;
import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.SubmissionAiReview;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.mapper.SubmissionAiReviewMapper;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.TaskSubmissionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

class SubmissionAiReviewServiceImplTest {

    private SubmissionAiReviewServiceImpl service;
    private TaskSubmissionService submissionService;
    private LearningTaskService taskService;
    private AgenticClient agenticClient;
    private Map<String, SubmissionAiReview> reviewStore;

    @BeforeEach
    void setUp() throws Exception {
        submissionService = mock(TaskSubmissionService.class);
        taskService = mock(LearningTaskService.class);
        agenticClient = mock(AgenticClient.class);
        reviewStore = new LinkedHashMap<>();

        SubmissionAiReviewMapper mapper = (SubmissionAiReviewMapper) Proxy.newProxyInstance(
                SubmissionAiReviewMapper.class.getClassLoader(),
                new Class<?>[]{SubmissionAiReviewMapper.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("insert".equals(name) && args != null && args.length == 1 && args[0] instanceof SubmissionAiReview r) {
                        if (r.getReviewId() == null) r.setReviewId("rev-" + (reviewStore.size() + 1));
                        reviewStore.put(r.getReviewId(), r);
                        return 1;
                    }
                    if ("toString".equals(name)) return "ReviewMapperProxy";
                    if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                    if ("equals".equals(name)) return proxy == args[0];
                    return null;
                });

        SubmissionAiReviewServiceImpl real = new SubmissionAiReviewServiceImpl(submissionService, taskService, agenticClient);
        com.neu.CoursePlatform.service.TeacherServiceTest.setBaseMapper(real, mapper);
        service = spy(real);

        // Stub getOne to return null (no existing review)
        lenient().doReturn(null).when(service).getOne(any(QueryWrapper.class));
    }

    // ============ generateReview ============

    @Test
    void generateReviewThrowsWhenSubmissionNotFound() {
        when(submissionService.getById("sub-x")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.generateReview("sub-x"));
    }

    @Test
    void generateReviewUsesAgenticForNonQuizTask() {
        TaskSubmission sub = submission("sub-1", "task-1", "报告内容不少于300字，包含实验结果和分析。");
        LearningTask task = task("task-1", "CS101", "report", "实验报告");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);

        Map<String, Object> aiData = new LinkedHashMap<>();
        aiData.put("score", 85);
        Map<String, Object> dims = new LinkedHashMap<>();
        dims.put("内容完整性", 88);
        dims.put("知识点覆盖度", 80);
        dims.put("逻辑结构", 85);
        dims.put("表达规范", 82);
        dims.put("任务要求符合度", 90);
        aiData.put("dimensions", dims);
        aiData.put("summary", "整体完成度好");
        aiData.put("suggestions", List.of("建议补充细节"));
        aiData.put("riskLevel", "low");
        AgenticResponse response = new AgenticResponse(true, aiData, "ok");
        when(agenticClient.invoke(eq("assessment"), any(AgenticRequest.class))).thenReturn(response);

        SubmissionAiReview review = service.generateReview("sub-1");

        assertNotNull(review);
        assertEquals("sub-1", review.getSubmissionId());
        assertEquals("generated", review.getStatus());
        assertEquals(85, review.getAiScore());
        assertEquals("low", review.getRiskLevel());
        assertTrue(reviewStore.containsKey(review.getReviewId()));
    }

    @Test
    void generateReviewFallsBackToLocalWhenAgenticFails() {
        TaskSubmission sub = submission("sub-1", "task-1",
                "本次实验通过Java实现了链表的基本操作。首先设计了Node类用于存储节点数据，" +
                "然后实现了LinkedList类包含插入、删除、查找等核心方法。最后通过测试用例验证了所有功能，" +
                "结果显示代码运行正常。实验中遇到的主要问题是空指针异常，通过添加边界检查解决。" +
                "总结：掌握了链表数据结构的实现原理。");
        LearningTask task = task("task-1", "CS101", "report", "实现链表数据结构");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);
        when(agenticClient.invoke(eq("assessment"), any(AgenticRequest.class)))
                .thenReturn(new AgenticResponse(false, Map.of(), "unavailable"));

        SubmissionAiReview review = service.generateReview("sub-1");

        assertNotNull(review);
        assertEquals("sub-1", review.getSubmissionId());
        assertEquals("generated", review.getStatus());
        assertNotNull(review.getAiScore());
        assertNotNull(review.getSummary());
        assertNotNull(review.getDimensions());
        assertNotNull(review.getSuggestions());
    }

    @Test
    void generateReviewFallsBackWhenAgenticReturnsNull() {
        TaskSubmission sub = submission("sub-1", "task-1", "一些内容足够长用来触发本地草稿逻辑一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十");
        LearningTask task = task("task-1", "CS101", "report", "任务描述");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);
        when(agenticClient.invoke(eq("assessment"), any(AgenticRequest.class))).thenReturn(null);

        SubmissionAiReview review = service.generateReview("sub-1");

        assertNotNull(review);
        assertEquals("generated", review.getStatus());
    }

    @Test
    void generateReviewFallsBackWhenAgenticThrows() {
        TaskSubmission sub = submission("sub-1", "task-1", "一些内容足够长用来触发本地草稿逻辑一二三四五六七八九十一二三四五六七八九十");
        LearningTask task = task("task-1", "CS101", "report", "任务描述");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);
        when(agenticClient.invoke(eq("assessment"), any(AgenticRequest.class)))
                .thenThrow(new RuntimeException("AI down"));

        SubmissionAiReview review = service.generateReview("sub-1");

        assertNotNull(review);
        assertEquals("generated", review.getStatus());
    }

    @Test
    void generateReviewLocalDraftScoresHighForLongContent() {
        TaskSubmission sub = submission("sub-1", "task-1",
                ("一、实验背景：本实验旨在实现链表数据结构的基本概念。" +
                 "二、实验过程：首先设计了Node类，包含data和next两个属性。" +
                 "然后实现了插入、删除、查找等核心方法。数据结构和代码分析显示，" +
                 "各函数的时间复杂度符合预期。最后通过测试验证了所有功能，" +
                 "结果显示代码运行正常。三、问题分析：实验中遇到的主要问题是空指针异常，" +
                 "通过添加边界检查和防御性编程解决。四、总结：掌握了链表数据结构的实现原理，" +
                 "加深了对指针和数据结构的理解。").repeat(3));
        LearningTask task = task("task-1", "CS101", "report", "实现链表数据结构");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);
        when(agenticClient.invoke(eq("assessment"), any(AgenticRequest.class)))
                .thenReturn(new AgenticResponse(false, Map.of(), "unavailable"));

        SubmissionAiReview review = service.generateReview("sub-1");

        assertTrue(review.getAiScore() >= 80);
        assertEquals("low", review.getRiskLevel());
    }

    @Test
    void generateReviewLocalDraftScoresLowForShortContent() {
        TaskSubmission sub = submission("sub-1", "task-1", "简短内容");
        LearningTask task = task("task-1", "CS101", "report", "实现链表");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);
        when(agenticClient.invoke(eq("assessment"), any(AgenticRequest.class)))
                .thenReturn(new AgenticResponse(false, Map.of(), "unavailable"));

        SubmissionAiReview review = service.generateReview("sub-1");

        assertTrue(review.getAiScore() < 80);
    }

    @Test
    void generateReviewLocalDraftWithFileButNoContent() {
        TaskSubmission sub = submission("sub-1", "task-1", "");
        sub.setFilePath("/files/report.pdf");
        LearningTask task = task("task-1", "CS101", "report", "实验报告");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);
        when(agenticClient.invoke(eq("assessment"), any(AgenticRequest.class)))
                .thenReturn(new AgenticResponse(false, Map.of(), "unavailable"));

        SubmissionAiReview review = service.generateReview("sub-1");

        assertNotNull(review);
        assertNotNull(review.getSummary());
    }

    @Test
    void generateReviewQuizTaskBuildsQuestionContent() {
        TaskSubmission sub = submission("sub-1", "task-1", "[{\"no\":\"q-1\",\"response\":\"我的答案\"}]");
        LearningTask task = task("task-1", "CS101", "quiz", "单元测验");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(true);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("submissionId", "sub-1");
        detail.put("details", List.of(
                Map.of("type", "fill", "stem", "填空题目", "correctAnswer", "正确答案",
                        "studentAnswer", "学生答案", "score", "5", "earnedScore", "0")
        ));
        when(submissionService.buildGradeDetail("sub-1")).thenReturn(detail);

        when(agenticClient.invoke(eq("assessment"), any(AgenticRequest.class)))
                .thenReturn(new AgenticResponse(false, Map.of(), "unavailable"));

        SubmissionAiReview review = service.generateReview("sub-1");

        assertNotNull(review);
        assertEquals("generated", review.getStatus());
    }

    @Test
    void generateReviewQuizTaskThrowsWhenNoAiReviewableQuestions() {
        TaskSubmission sub = submission("sub-1", "task-1", "[{\"no\":\"q-1\",\"response\":\"A\"}]");
        LearningTask task = task("task-1", "CS101", "quiz", "单元测验");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(true);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("details", List.of(
                Map.of("type", "single", "stem", "单选题", "correctAnswer", "A",
                        "studentAnswer", "A", "score", "5", "earnedScore", "5")
        ));
        when(submissionService.buildGradeDetail("sub-1")).thenReturn(detail);

        assertThrows(IllegalArgumentException.class, () -> service.generateReview("sub-1"));
    }

    @Test
    void generateReviewQuizTaskThrowsWhenDetailsMissing() {
        TaskSubmission sub = submission("sub-1", "task-1", "[{}]");
        LearningTask task = task("task-1", "CS101", "quiz", "测验");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(true);
        when(submissionService.buildGradeDetail("sub-1")).thenReturn(Map.of());

        assertThrows(IllegalArgumentException.class, () -> service.generateReview("sub-1"));
    }

    @Test
    void generateReviewAgenticResponseParsedCorrectly() {
        TaskSubmission sub = submission("sub-1", "task-1", "一些报告内容足以满足长度要求一二三四五六七八九十一二三四五六七八九十");
        LearningTask task = task("task-1", "CS101", "report", "实验");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);

        // Response with nested data structure
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("score", 72);
        Map<String, Object> dims = new LinkedHashMap<>();
        dims.put("内容完整性", 70);
        dims.put("知识点覆盖度", 75);
        dims.put("逻辑结构", 68);
        dims.put("表达规范", 73);
        dims.put("任务要求符合度", 74);
        inner.put("dimensions", dims);
        inner.put("summary", "");
        inner.put("suggestions", List.of());
        inner.put("riskLevel", "medium");
        AgenticResponse response = new AgenticResponse(true, inner, "ok");
        when(agenticClient.invoke(eq("assessment"), any(AgenticRequest.class))).thenReturn(response);

        SubmissionAiReview review = service.generateReview("sub-1");

        assertEquals(72, review.getAiScore());
        assertEquals("medium", review.getRiskLevel());
    }

    @Test
    void generateReviewParsesFencedAgenticDataAndNormalizesInvalidRisk() {
        TaskSubmission sub = submission("sub-1", "task-1", "报告内容足够长，包含实验、结果、分析、代码和数据，能够触发AI评阅解析逻辑。");
        LearningTask task = task("task-1", "CS101", "report", "实验");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);

        Map<String, Object> aiData = new LinkedHashMap<>();
        aiData.put("score", 88);
        aiData.put("dimensions", Map.of());
        aiData.put("summary", "");
        aiData.put("suggestions", List.of());
        aiData.put("riskLevel", "unknown");
        when(agenticClient.invoke(eq("assessment"), any(AgenticRequest.class)))
                .thenReturn(new AgenticResponse(true, aiData, "ok"));

        SubmissionAiReview review = service.generateReview("sub-1");

        assertEquals(88, review.getAiScore());
        assertEquals("low", review.getRiskLevel());
        assertFalse(review.getSummary().isBlank());
    }

    @Test
    void getLatestBySubmissionIdBuildsQuery() {
        assertNull(service.getLatestBySubmissionId("sub-1"));
    }

    @Test
    void privateParseDraftHandlesFencedJsonAndMissingScore() throws Exception {
        Object draft = invokePrivate("parseDraft", new Class<?>[]{String.class}, """
                ```json
                {
                  "dimensions": {
                    "内容完整性": 100,
                    "知识点覆盖度": 80,
                    "逻辑结构": 60,
                    "表达规范": 40,
                    "任务要求符合度": 20
                  },
                  "suggestions": ["继续完善"],
                  "riskLevel": "medium",
                  "summary": "结构清楚"
                }
                ```
                """);

        java.lang.reflect.Method score = draft.getClass().getDeclaredMethod("score");
        score.setAccessible(true);
        assertEquals(60, score.invoke(draft));
    }

    @Test
    void privateToJsonFallsBackToEmptyArrayWhenSerializationFails() throws Exception {
        Map<String, Object> cyclic = new LinkedHashMap<>();
        cyclic.put("self", cyclic);

        String json = (String) invokePrivate("toJson", new Class<?>[]{Object.class}, cyclic);

        assertEquals("[]", json);
    }

    @Test
    void generateReviewAgenticFallsBackWhenDataEmpty() {
        TaskSubmission sub = submission("sub-1", "task-1", "足够的报告内容长度一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十");
        LearningTask task = task("task-1", "CS101", "report", "任务");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);
        when(agenticClient.invoke(eq("assessment"), any(AgenticRequest.class)))
                .thenReturn(new AgenticResponse(true, Map.of(), "ok"));

        SubmissionAiReview review = service.generateReview("sub-1");

        assertNotNull(review);
        assertEquals("generated", review.getStatus());
    }

    // ============ helpers ============

    private static TaskSubmission submission(String id, String taskNo, String content) {
        TaskSubmission s = new TaskSubmission();
        s.setSubmissionId(id);
        s.setTaskNo(taskNo);
        s.setStudentNo("2024001");
        s.setContent(content);
        return s;
    }

    private static LearningTask task(String taskNo, String courseCode, String type, String description) {
        LearningTask t = new LearningTask();
        t.setTaskNo(taskNo);
        t.setCourseCode(courseCode);
        t.setTaskType(type);
        t.setDescription(description);
        return t;
    }

    private Object invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        java.lang.reflect.Method method = SubmissionAiReviewServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(service, args);
    }
}

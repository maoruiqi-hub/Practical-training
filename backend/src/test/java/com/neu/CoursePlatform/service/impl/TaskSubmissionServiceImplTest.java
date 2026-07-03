package com.neu.CoursePlatform.service.impl;

import static com.neu.CoursePlatform.service.TeacherServiceTest.setBaseMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.common.event.GameEventPublisher;
import com.neu.CoursePlatform.dto.TaskSubmissionDTO;
import com.neu.CoursePlatform.entity.*;
import com.neu.CoursePlatform.mapper.TaskSubmissionMapper;
import com.neu.CoursePlatform.service.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.*;

class TaskSubmissionServiceImplTest {

    private TaskSubmissionServiceImpl service;
    private Map<String, TaskSubmission> submissionStore;
    private LearningTaskService taskService;
    private StudentService studentService;
    private QuestionService questionService;
    private SubmissionAnswerService answerService;
    private KnowledgePointService knowledgePointService;
    private CourseGameConfigService gameConfigService;
    private FloorProgressService floorProgressService;
    private GameEventPublisher gameEventPublisher;

    @BeforeEach
    void setUp() throws Exception {
        submissionStore = new LinkedHashMap<>();

        TaskSubmissionMapper submissionMapper = (TaskSubmissionMapper) Proxy.newProxyInstance(
                TaskSubmissionMapper.class.getClassLoader(),
                new Class<?>[]{TaskSubmissionMapper.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("insert".equals(methodName) && args != null && args.length == 1
                            && args[0] instanceof TaskSubmission) {
                        TaskSubmission s = (TaskSubmission) args[0];
                        if (s.getSubmissionId() == null) s.setSubmissionId("sub-" + (submissionStore.size() + 1));
                        submissionStore.put(s.getSubmissionId(), s);
                        return 1;
                    }
                    if ("selectByStudentNo".equals(methodName)) {
                        return submissionStore.values().stream()
                                .filter(s -> args[0].equals(s.getStudentNo())).toList();
                    }
                    if ("selectByTaskNo".equals(methodName)) {
                        return submissionStore.values().stream()
                                .filter(s -> args[0].equals(s.getTaskNo())).toList();
                    }
                    if ("selectById".equals(methodName)) return submissionStore.get(String.valueOf(args[0]));
                    if ("selectCount".equals(methodName)) return (long) submissionStore.size();
                    if ("updateById".equals(methodName) && args != null && args.length >= 1
                            && args[0] instanceof TaskSubmission) {
                        TaskSubmission s = (TaskSubmission) args[0];
                        if (submissionStore.containsKey(s.getSubmissionId())) {
                            submissionStore.put(s.getSubmissionId(), s);
                            return 1;
                        }
                        return 0;
                    }
                    if ("selectList".equals(methodName)) {
                        if (args != null && args.length >= 1 && args[0] instanceof com.baomidou.mybatisplus.core.conditions.query.QueryWrapper) {
                            // Simple filtering for QueryWrapper - return all for now
                            return new ArrayList<>(submissionStore.values());
                        }
                        return new ArrayList<>(submissionStore.values());
                    }
                    if ("toString".equals(methodName)) return "SubmissionMapperProxy";
                    if ("hashCode".equals(methodName)) return System.identityHashCode(proxy);
                    if ("equals".equals(methodName)) return proxy == args[0];
                    return null;
                });

        taskService = mock(LearningTaskService.class);
        studentService = mock(StudentService.class);
        questionService = mock(QuestionService.class);
        answerService = mock(SubmissionAnswerService.class);
        knowledgePointService = mock(KnowledgePointService.class);
        gameConfigService = mock(CourseGameConfigService.class);
        floorProgressService = mock(FloorProgressService.class);
        gameEventPublisher = mock(GameEventPublisher.class);

        lenient().when(gameConfigService.isEnabled(anyString())).thenReturn(false);

        service = new TaskSubmissionServiceImpl(taskService, studentService, questionService,
                answerService, knowledgePointService, gameConfigService,
                floorProgressService, gameEventPublisher);
        setBaseMapper(service, submissionMapper);
    }

    // ============ listByStudentNo ============

    @Test
    void listByStudentNoReturnsSubmissions() {
        submissionStore.put("s1", submission("s1", "task-1", "2024001", "submitted"));
        submissionStore.put("s2", submission("s2", "task-2", "2024001", "graded"));
        submissionStore.put("s3", submission("s3", "task-1", "2024002", "submitted"));

        List<TaskSubmission> result = service.listByStudentNo("2024001");
        assertEquals(2, result.size());
    }

    @Test
    void listByStudentNoReturnsEmptyWhenNoSubmissions() {
        List<TaskSubmission> result = service.listByStudentNo("9999999");
        assertTrue(result.isEmpty());
    }

    // ============ listByTaskNo ============

    @Test
    void listByTaskNoReturnsSubmissions() {
        submissionStore.put("s1", submission("s1", "task-1", "2024001", "submitted"));
        submissionStore.put("s2", submission("s2", "task-1", "2024002", "submitted"));

        List<TaskSubmission> result = service.listByTaskNo("task-1");
        assertEquals(2, result.size());
    }

    // ============ isTaskOverdue ============

    @Test
    void isTaskOverdueReturnsTrueWhenDeadlinePassed() {
        LearningTask task = task("task-1", "CS101", "quiz", null);
        task.setDeadline(LocalDateTime.now().minusDays(1));
        when(taskService.getById("task-1")).thenReturn(task);

        assertTrue(service.isTaskOverdue("task-1"));
    }

    @Test
    void isTaskOverdueReturnsFalseWhenDeadlineFuture() {
        LearningTask task = task("task-1", "CS101", "quiz", null);
        task.setDeadline(LocalDateTime.now().plusDays(1));
        when(taskService.getById("task-1")).thenReturn(task);

        assertFalse(service.isTaskOverdue("task-1"));
    }

    @Test
    void isTaskOverdueReturnsFalseWhenTaskNotFound() {
        when(taskService.getById("task-nonexistent")).thenReturn(null);
        assertFalse(service.isTaskOverdue("task-nonexistent"));
    }

    // ============ hasSubmitted ============

    @Test
    void hasSubmittedReturnsTrueWhenSubmissionExists() {
        submissionStore.put("s1", submission("s1", "task-1", "2024001", "submitted"));
        assertTrue(service.hasSubmitted("task-1", "2024001"));
    }

    @Test
    void hasSubmittedReturnsFalseWhenNoSubmission() {
        assertFalse(service.hasSubmitted("task-1", "2024001"));
    }

    // ============ getTaskCourseCode ============

    @Test
    void getTaskCourseCodeReturnsCourseCode() {
        when(taskService.getById("task-1")).thenReturn(task("task-1", "CS101", "quiz", null));
        assertEquals("CS101", service.getTaskCourseCode("task-1"));
    }

    @Test
    void getTaskCourseCodeReturnsNullWhenTaskNotFound() {
        when(taskService.getById("task-nonexistent")).thenReturn(null);
        assertNull(service.getTaskCourseCode("task-nonexistent"));
    }

    // ============ applyInitialGrading (quiz type) ============

    @Test
    void applyInitialGradingForQuizTaskWithAutoGradableQuestions() {
        LearningTask task = task("task-1", "CS101", "quiz", null);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(true);
        Question q = question("q-1", "single", "A", "[\"选项A\",\"选项B\"]");
        q.setScore(5);
        when(questionService.getById("q-1")).thenReturn(q);

        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");
        sub.setContent("[{\"no\":\"q-1\",\"response\":\"A\"}]");

        service.applyInitialGrading(sub);

        assertTrue(sub.getStatus().equals("graded") || sub.getStatus().equals("submitted"));
    }

    @Test
    void applyInitialGradingForQuizTaskWithManualQuestions() {
        LearningTask task = task("task-1", "CS101", "quiz", null);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(true);
        // essay type question is not auto-gradable
        Question q = question("q-2", "essay", "", "");
        q.setScore(10);
        when(questionService.getById("q-2")).thenReturn(q);

        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");
        sub.setContent("[{\"no\":\"q-2\",\"response\":\"我的答案\"}]");

        service.applyInitialGrading(sub);

        assertEquals("submitted", sub.getStatus());
        assertTrue(sub.getFeedback().contains("待教师复核"));
    }

    @Test
    void applyInitialGradingForVideoAutoCompletes() {
        LearningTask task = task("task-1", "CS101", "video", null);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);

        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");

        service.applyInitialGrading(sub);

        assertEquals("graded", sub.getStatus());
        assertEquals("系统自动记录完成", sub.getFeedback());
    }

    @Test
    void applyInitialGradingForReadingAutoCompletes() {
        LearningTask task = task("task-1", "CS101", "reading", null);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);

        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");

        service.applyInitialGrading(sub);

        assertEquals("graded", sub.getStatus());
    }

    @Test
    void applyInitialGradingForReportRequiresManualReview() {
        LearningTask task = task("task-1", "CS101", "report", null);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);

        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");

        service.applyInitialGrading(sub);

        assertEquals("submitted", sub.getStatus());
        assertEquals("待教师评阅", sub.getFeedback());
    }

    @Test
    void applyInitialGradingWhenTaskNotFound() {
        when(taskService.getById("task-1")).thenReturn(null);

        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");

        service.applyInitialGrading(sub);

        assertEquals("submitted", sub.getStatus());
    }

    @Test
    void applyInitialGradingForQuizWithNullContent() {
        LearningTask task = task("task-1", "CS101", "quiz", null);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(true);

        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");

        // parseQuizAnswers will throw for null content
        assertThrows(IllegalArgumentException.class, () -> service.applyInitialGrading(sub));
    }

    @Test
    void applyInitialGradingForQuizThrowsWhenAnswerItemIsNotObject() {
        LearningTask task = task("task-1", "CS101", "quiz", null);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(true);

        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");
        sub.setContent("[1]");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.applyInitialGrading(sub));
        assertTrue(ex.getMessage().contains("答题格式错误"));
    }

    @Test
    void applyInitialGradingForQuizThrowsWhenAnswerNoIsMissing() {
        LearningTask task = task("task-1", "CS101", "quiz", null);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(true);

        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");
        sub.setContent("[{\"response\":\"A\"}]");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.applyInitialGrading(sub));
        assertTrue(ex.getMessage().contains("缺少题目编号"));
    }

    @Test
    void applyInitialGradingForQuizThrowsWhenContentIsNotArray() {
        LearningTask task = task("task-1", "CS101", "quiz", null);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(true);

        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");
        sub.setContent("{\"no\":\"q-1\"}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.applyInitialGrading(sub));
        assertTrue(ex.getMessage().contains("答题格式错误"));
    }

    // ============ autoScoreChoices ============

    @Test
    void autoScoreChoicesReturnsZeroForNonQuizSubmission() {
        LearningTask task = task("task-1", "CS101", "video", null);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);

        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");

        int score = service.autoScoreChoices(sub);
        assertEquals(0, score);
    }

    // ============ supersedePrevious ============

    @Test
    void supersedePreviousMarksOldSubmissionsAsSuperseded() {
        TaskSubmission old1 = submission("s1", "task-1", "2024001", "submitted");
        TaskSubmission old2 = submission("s2", "task-1", "2024001", "submitted");
        submissionStore.put("s1", old1);
        submissionStore.put("s2", old2);

        service.supersedePrevious("task-1", "2024001");

        assertEquals("superseded", old1.getStatus());
        assertEquals("superseded", old2.getStatus());
    }

    // ============ listDtoByTaskNo ============

    @Test
    void listDtoByTaskNoFiltersSuperseded() {
        LearningTask task = task("task-1", "CS101", "quiz", null);
        when(taskService.getById("task-1")).thenReturn(task);
        Student stu = new Student();
        stu.setName("张三");
        lenient().when(studentService.getById(anyString())).thenReturn(stu);

        TaskSubmission sub = submission("s1", "task-1", "2024001", "graded");
        sub.setAttemptNumber(1);
        submissionStore.put("s1", sub);
        TaskSubmission superseded = submission("s2", "task-1", "2024001", "superseded");
        superseded.setAttemptNumber(2);
        submissionStore.put("s2", superseded);

        List<TaskSubmissionDTO> result = service.listDtoByTaskNo("task-1");

        assertEquals(1, result.size());
        assertEquals("s1", result.get(0).getSubmissionId());
    }

    // ============ buildGradeDetail ============

    @Test
    void buildGradeDetailReturnsNullForMissingSubmission() {
        assertNull(service.buildGradeDetail("nonexistent"));
    }

    @Test
    void buildGradeDetailReturnsDetailsForExistingSubmission() {
        TaskSubmission sub = submission("s1", "task-1", "2024001", "graded");
        sub.setContent("[{\"no\":\"q-1\",\"response\":\"A\"}]");
        submissionStore.put("s1", sub);

        LearningTask task = task("task-1", "CS101", "quiz", null);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(true);
        Question q = question("q-1", "single", "A", "[\"选项A\",\"选项B\"]");
        when(questionService.getById("q-1")).thenReturn(q);
        lenient().when(answerService.listBySubmissionId(anyString())).thenReturn(List.of());

        Map<String, Object> detail = service.buildGradeDetail("s1");

        assertNotNull(detail);
        assertEquals("s1", detail.get("submissionId"));
        assertEquals("2024001", detail.get("studentNo"));
        assertNotNull(detail.get("details"));
    }

    @Test
    void buildGradeDetailReturnsParseErrorForMalformedQuizAnswers() {
        TaskSubmission sub = submission("s-parse", "task-1", "2024001", "graded");
        sub.setContent("[1]");
        lenient().when(answerService.listBySubmissionId(anyString())).thenReturn(List.of());

        List<Map<String, Object>> details = invokeBuildAnswerDetails(sub);

        assertTrue(String.valueOf(details.get(0).get("parseError")).contains("答题格式错误"));
    }

    // ============ submitWithGrading ============

    @Test
    void submitWithGradingForVideoSavesAndAutoCompletes() {
        LearningTask task = task("task-1", "CS101", "video", null);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);

        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");
        sub.setStudentNo("2024001");

        service.submitWithGrading(sub);

        assertNotNull(sub.getSubmissionId());
        assertEquals("graded", sub.getStatus());
        assertTrue(submissionStore.containsKey(sub.getSubmissionId()));
    }

    @Test
    void submitWithGradingForQuizSavesAnswerDetails() {
        LearningTask task = task("task-quiz", "CS101", "quiz", "随堂测验");
        when(taskService.getById("task-quiz")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(true);
        Question single = question("q-1", "single", "A", "[\"正确\",\"错误\"]");
        single.setStem("选择题");
        single.setKnowledgePointId("kp-1");
        Question fill = question("q-2", "fill", "range", "");
        fill.setStem("填空题");
        fill.setKnowledgePointId("kp-2");
        when(questionService.getById("q-1")).thenReturn(single);
        when(questionService.getById("q-2")).thenReturn(fill);
        when(answerService.listBySubmissionId(anyString())).thenReturn(List.of());

        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-quiz");
        sub.setStudentNo("2024001");
        sub.setContent("[{\"no\":\"q-1\",\"response\":\"正确\"},{\"no\":\"q-2\",\"response\":\" range \"}]");

        service.submitWithGrading(sub);

        assertEquals("graded", sub.getStatus());
        assertEquals(10, sub.getScore());
        ArgumentCaptor<List<SubmissionAnswer>> captor = ArgumentCaptor.forClass(List.class);
        verify(answerService).saveBatch(captor.capture());
        List<SubmissionAnswer> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertTrue(saved.stream().allMatch(answer -> Boolean.TRUE.equals(answer.getCorrect())));
        assertEquals("kp-1", saved.get(0).getKnowledgePointId());
    }

    @Test
    void buildGradeDetailUsesSavedAnswersAndKnowledgeNames() {
        TaskSubmission sub = submission("s-detail", "task-1", "2024001", "graded");
        sub.setContent("非测验文本");
        submissionStore.put("s-detail", sub);
        LearningTask task = task("task-1", "CS101", "homework", "课后作业");
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);
        SubmissionAnswer answer = new SubmissionAnswer();
        answer.setQuestionId("q-1");
        answer.setQuestionStem("保存题干");
        answer.setQuestionType("essay");
        answer.setKnowledgePointId("kp-1");
        answer.setStudentAnswer("学生答案");
        answer.setCorrectAnswer("参考答案");
        answer.setScore(8);
        answer.setMaxScore(10);
        answer.setAutoGradable(false);
        when(answerService.listBySubmissionId("s-detail")).thenReturn(List.of(answer));
        KnowledgePoint point = new KnowledgePoint();
        point.setKnowledgePointId("kp-1");
        point.setName("循环");
        when(knowledgePointService.getById("kp-1")).thenReturn(point);

        Map<String, Object> detail = service.buildGradeDetail("s-detail");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> details = (List<Map<String, Object>>) detail.get("details");
        assertEquals("课后作业", detail.get("taskName"));
        assertEquals("循环", details.get(0).get("knowledgePointName"));
        assertEquals(8, details.get(0).get("earnedScore"));
        assertEquals(false, details.get(0).get("autoGradable"));
    }

    @Test
    void publishAssessmentResultEventsPublishesAnswerFloorAndBossEvents() {
        LearningTask task = task("boss-task", "CS101", "boss", "Boss测验");
        when(taskService.getById("boss-task")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(true);
        when(gameConfigService.isEnabled("CS101")).thenReturn(true);
        SubmissionAnswer correct = answer("a1", "q-1", "kp-1", true, 5);
        SubmissionAnswer wrong = answer("a2", "q-2", "kp-1", false, 5);
        when(answerService.listBySubmissionId("sub-boss")).thenReturn(List.of(correct, wrong));
        when(answerService.listByStudentNo(anyString(), isNull(), anyString(), isNull()))
                .thenReturn(List.of(correct, wrong));
        Question q1 = question("q-1", "single", "A", "[\"A\",\"B\"]");
        q1.setDifficulty(3);
        Question q2 = question("q-2", "single", "B", "[\"A\",\"B\"]");
        q2.setDifficulty(2);
        when(questionService.getById("q-1")).thenReturn(q1);
        when(questionService.getById("q-2")).thenReturn(q2);
        TaskSubmission sub = submission("sub-boss", "boss-task", "2024001", "graded");

        service.publishAssessmentResultEvents(sub);

        ArgumentCaptor<com.neu.CoursePlatform.common.event.GameEvent> eventCaptor =
                ArgumentCaptor.forClass(com.neu.CoursePlatform.common.event.GameEvent.class);
        verify(gameEventPublisher, times(2)).publish(eventCaptor.capture());
        assertTrue(eventCaptor.getAllValues().stream().anyMatch(event -> "answer_correct".equals(event.getEventType())));
        assertTrue(eventCaptor.getAllValues().stream().anyMatch(event -> "answer_wrong".equals(event.getEventType())));
        verify(floorProgressService).recordQuizResult("2024001", "CS101", "kp-1", "sub-boss", false, 10);
    }

    @Test
    void publishAssessmentResultEventsPublishesBossDefeatedWhenAllCorrect() {
        LearningTask task = task("boss-task", "CS101", "boss_exam", "Boss测验");
        when(taskService.getById("boss-task")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(true);
        when(gameConfigService.isEnabled("CS101")).thenReturn(true);
        SubmissionAnswer correct = answer("a1", "q-1", "kp-1", true, 5);
        when(answerService.listBySubmissionId("sub-boss")).thenReturn(List.of(correct));
        when(answerService.listByStudentNo(anyString(), isNull(), anyString(), isNull())).thenReturn(List.of(correct));
        when(questionService.getById("q-1")).thenReturn(question("q-1", "single", "A", "[\"A\",\"B\"]"));
        TaskSubmission sub = submission("sub-boss", "boss-task", "2024001", "graded");

        service.publishAssessmentResultEvents(sub);

        ArgumentCaptor<com.neu.CoursePlatform.common.event.GameEvent> eventCaptor =
                ArgumentCaptor.forClass(com.neu.CoursePlatform.common.event.GameEvent.class);
        verify(gameEventPublisher, times(2)).publish(eventCaptor.capture());
        assertTrue(eventCaptor.getAllValues().stream().anyMatch(event -> "boss_defeated".equals(event.getEventType())));
    }

    // ============ countByStudentAndTask ============

    @Test
    void countByStudentAndTaskReturnsCount() {
        submissionStore.put("s1", submission("s1", "task-1", "2024001", "submitted"));
        submissionStore.put("s2", submission("s2", "task-1", "2024001", "graded"));

        int count = service.countByStudentAndTask("task-1", "2024001");
        assertEquals(2, count);
    }

    // ============ helpers ============

    private static TaskSubmission submission(String id, String taskNo, String studentNo, String status) {
        TaskSubmission s = new TaskSubmission();
        s.setSubmissionId(id);
        s.setTaskNo(taskNo);
        s.setStudentNo(studentNo);
        s.setStatus(status);
        s.setSubmitTime(LocalDateTime.now());
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

    private static Question question(String id, String type, String answer, String options) {
        Question q = new Question();
        q.setQuestionId(id);
        q.setType(type);
        q.setAnswer(answer);
        q.setOptions(options);
        q.setScore(5);
        return q;
    }

    private static SubmissionAnswer answer(String id, String questionId, String knowledgePointId, boolean correct, int maxScore) {
        SubmissionAnswer answer = new SubmissionAnswer();
        answer.setId(id);
        answer.setSubmissionId("sub-boss");
        answer.setTaskNo("boss-task");
        answer.setStudentNo("2024001");
        answer.setQuestionId(questionId);
        answer.setKnowledgePointId(knowledgePointId);
        answer.setAutoGradable(true);
        answer.setCorrect(correct);
        answer.setMaxScore(maxScore);
        answer.setScore(correct ? maxScore : 0);
        return answer;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> invokeBuildAnswerDetails(TaskSubmission sub) {
        try {
            java.lang.reflect.Method method = TaskSubmissionServiceImpl.class
                    .getDeclaredMethod("buildAnswerDetails", TaskSubmission.class);
            method.setAccessible(true);
            return (List<Map<String, Object>>) method.invoke(service, sub);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // Helper: get baseMapper via reflection
    private static com.baomidou.mybatisplus.core.mapper.BaseMapper<?> getBaseMapper(Object serviceInstance) {
        try {
            for (Class<?> clazz = serviceInstance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
                try {
                    java.lang.reflect.Field field = clazz.getDeclaredField("baseMapper");
                    field.setAccessible(true);
                    return (com.baomidou.mybatisplus.core.mapper.BaseMapper<?>) field.get(serviceInstance);
                } catch (NoSuchFieldException e) {
                    // continue to superclass
                }
            }
            throw new NoSuchFieldException("baseMapper not found in hierarchy");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

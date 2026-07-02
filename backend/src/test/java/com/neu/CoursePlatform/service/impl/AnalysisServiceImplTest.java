package com.neu.CoursePlatform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.SubmissionAnswer;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.SubmissionAnswerService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

class AnalysisServiceImplTest {

    private AnalysisServiceImpl service;
    private SubmissionAnswerService answerService;
    private KnowledgePointService knowledgePointService;
    private LearningTaskService taskService;

    @BeforeEach
    void setUp() {
        answerService = mock(SubmissionAnswerService.class);
        knowledgePointService = mock(KnowledgePointService.class);
        taskService = mock(LearningTaskService.class);
        service = new AnalysisServiceImpl(answerService, knowledgePointService, taskService);
    }

    private static int asInt(Object v) { return ((Number) v).intValue(); }

    // ============ buildStudentWrongStats ============

    @Test
    void buildStudentWrongStatsDelegatesToAnswerService() {
        when(answerService.listByStudentNo("2024001", "task-1", null, null)).thenReturn(List.of());

        Map<String, Object> result = service.buildStudentWrongStats("2024001", "task-1", null, null);

        assertNotNull(result);
        assertEquals(0, asInt(result.get("totalAnswers")));
        assertEquals(0L, result.get("wrongAnswers"));
    }

    @Test
    void buildStudentWrongStatsCountsWrongAnswers() {
        SubmissionAnswer wrong = answer("a1", "q-1", "single", true, false);
        SubmissionAnswer correct = answer("a2", "q-2", "single", true, true);
        when(answerService.listByStudentNo("2024001", "task-1", null, null))
                .thenReturn(List.of(wrong, correct));

        Map<String, Object> result = service.buildStudentWrongStats("2024001", "task-1", null, null);

        assertEquals(2, asInt(result.get("totalAnswers")));
        assertEquals(1L, result.get("wrongAnswers"));
    }

    // ============ buildTaskWrongStats ============

    @Test
    void buildTaskWrongStatsDelegatesToAnswerService() {
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of());

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        assertNotNull(result);
        assertEquals(0, asInt(result.get("totalAnswers")));
    }

    // ============ buildCourseWrongStats ============

    @Test
    void buildCourseWrongStatsAggregatesAcrossQuizTasks() {
        LearningTask task1 = task("task-1", "CS101", "quiz");
        LearningTask task2 = task("task-2", "CS101", "video");
        when(taskService.listByCourseCode("CS101")).thenReturn(List.of(task1, task2));
        when(taskService.isQuizTask(task1)).thenReturn(true);
        when(taskService.isQuizTask(task2)).thenReturn(false);
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of(
                answer("a1", "q-1", "single", true, false)));
        when(answerService.listByTaskNo("task-2")).thenReturn(List.of());

        Map<String, Object> result = service.buildCourseWrongStats("CS101");

        assertEquals(1, asInt(result.get("totalAnswers")));
    }

    // ============ buildWrongStats aggregation ============

    @Test
    void buildStatsAggregatesByQuestion() {
        SubmissionAnswer a1 = answer("a1", "q-1", "single", true, false);
        SubmissionAnswer a2 = answer("a2", "q-1", "single", true, true);
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of(a1, a2));

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byQuestion = (List<Map<String, Object>>) result.get("byQuestion");
        assertEquals(1, byQuestion.size());
        assertEquals("q-1", byQuestion.get(0).get("key"));
        assertEquals(2, asInt(byQuestion.get(0).get("total")));
        assertEquals(1L, byQuestion.get(0).get("wrong"));
    }

    @Test
    void buildStatsAggregatesByType() {
        SubmissionAnswer a1 = answer("a1", "q-1", "single", true, false);
        SubmissionAnswer a2 = answer("a2", "q-2", "multi", true, true);
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of(a1, a2));

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byType = (List<Map<String, Object>>) result.get("byType");
        assertEquals(2, byType.size());
    }

    @Test
    void buildStatsAggregatesByKnowledgePoint() {
        SubmissionAnswer a1 = answer("a1", "q-1", "single", true, false);
        a1.setKnowledgePointId("kp-1");
        SubmissionAnswer a2 = answer("a2", "q-2", "single", true, true);
        a2.setKnowledgePointId("kp-1");
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of(a1, a2));
        when(knowledgePointService.getById("kp-1")).thenReturn(kp("kp-1", "Java基础"));

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byKp = (List<Map<String, Object>>) result.get("byKnowledgePoint");
        assertEquals(1, byKp.size());
        assertEquals("Java基础", byKp.get(0).get("key"));
        assertEquals(2, asInt(byKp.get(0).get("total")));
        assertEquals(1L, byKp.get(0).get("wrong"));
    }

    @Test
    void buildStatsKnowledgePointUnknownNameReturnsUnclassified() {
        SubmissionAnswer a1 = answer("a1", "q-1", "single", true, false);
        a1.setKnowledgePointId("kp-missing");
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of(a1));
        when(knowledgePointService.getById("kp-missing")).thenReturn(null);

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byKp = (List<Map<String, Object>>) result.get("byKnowledgePoint");
        assertEquals("未分类", byKp.get(0).get("key"));
    }

    @Test
    void buildStatsNullKnowledgePointIdAsUnclassified() {
        SubmissionAnswer a1 = answer("a1", "q-1", null, true, false);
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of(a1));

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byType = (List<Map<String, Object>>) result.get("byType");
        assertEquals("未分类", byType.get(0).get("key"));
    }

    // ============ wrongList ============

    @Test
    void buildStatsWrongListOnlyContainsWrongAnswers() {
        SubmissionAnswer wrong = answer("a1", "q-1", "single", true, false);
        SubmissionAnswer correct = answer("a2", "q-2", "single", true, true);
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of(wrong, correct));

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wrongList = (List<Map<String, Object>>) result.get("wrongList");
        assertEquals(1, wrongList.size());
        assertEquals("sub-a1", wrongList.get(0).get("submissionId"));
    }

    @Test
    void buildStatsWrongListIncludesQuestionDetails() {
        SubmissionAnswer wrong = answer("a1", "q-1", "single", true, false);
        wrong.setQuestionStem("什么是Java？");
        wrong.setKnowledgePointId("kp-1");
        wrong.setStudentAnswer("B");
        wrong.setCorrectAnswer("A");
        wrong.setScore(0);
        wrong.setMaxScore(5);
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of(wrong));
        when(knowledgePointService.getById("kp-1")).thenReturn(kp("kp-1", "Java基础"));

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wrongList = (List<Map<String, Object>>) result.get("wrongList");
        assertEquals("什么是Java？", wrongList.get(0).get("stem"));
        assertEquals("Java基础", wrongList.get(0).get("knowledgePointName"));
        assertEquals("B", wrongList.get(0).get("studentAnswer"));
        assertEquals("A", wrongList.get(0).get("correctAnswer"));
        assertEquals(0, wrongList.get(0).get("score"));
        assertEquals(5, wrongList.get(0).get("maxScore"));
    }

    // ============ mastery ============

    @Test
    void buildStatsMasteryRatesLevels() {
        SubmissionAnswer right = answer("a1", "q-1", "single", true, true);
        right.setKnowledgePointId("kp-1");
        SubmissionAnswer wrong = answer("a2", "q-2", "single", true, false);
        wrong.setKnowledgePointId("kp-2");
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of(right, wrong));
        when(knowledgePointService.getById("kp-1")).thenReturn(kp("kp-1", "已掌握"));
        when(knowledgePointService.getById("kp-2")).thenReturn(kp("kp-2", "薄弱点"));

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mastery = (List<Map<String, Object>>) result.get("mastery");
        assertEquals(2, mastery.size());
        assertEquals("薄弱点", mastery.get(0).get("knowledgePointName"));
        assertEquals(0.0, mastery.get(0).get("masteryRate"));
        assertEquals("薄弱", mastery.get(0).get("level"));
        assertEquals("已掌握", mastery.get(1).get("knowledgePointName"));
        assertEquals(100.0, mastery.get(1).get("masteryRate"));
        assertEquals("掌握较好", mastery.get(1).get("level"));
    }

    @Test
    void buildStatsMasteryLevelBasic() {
        SubmissionAnswer a1 = answer("a1", "q-1", "single", true, false);
        a1.setKnowledgePointId("kp-1");
        SubmissionAnswer a2 = answer("a2", "q-2", "single", true, true);
        a2.setKnowledgePointId("kp-1");
        SubmissionAnswer a3 = answer("a3", "q-3", "single", true, true);
        a3.setKnowledgePointId("kp-1");
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of(a1, a2, a3));
        when(knowledgePointService.getById("kp-1")).thenReturn(kp("kp-1", "基础"));

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mastery = (List<Map<String, Object>>) result.get("mastery");
        assertEquals("需要巩固", mastery.get(0).get("level"));
    }

    // ============ recommendations ============

    @Test
    void buildStatsRecommendationsForWeakPoints() {
        SubmissionAnswer a1 = answer("a1", "q-1", "single", true, false);
        a1.setKnowledgePointId("kp-1");
        SubmissionAnswer a2 = answer("a2", "q-2", "single", true, false);
        a2.setKnowledgePointId("kp-1");
        SubmissionAnswer a3 = answer("a3", "q-3", "single", true, true);
        a3.setKnowledgePointId("kp-1");
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of(a1, a2, a3));
        when(knowledgePointService.getById("kp-1")).thenReturn(kp("kp-1", "薄弱点"));

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recs = (List<Map<String, Object>>) result.get("recommendations");
        assertEquals(1, recs.size());
        assertEquals("high", recs.get(0).get("priority"));
        assertEquals("薄弱点", recs.get(0).get("knowledgePointName"));
    }

    @Test
    void buildStatsRecommendationsSkippedWhenTotalTooSmall() {
        SubmissionAnswer a1 = answer("a1", "q-1", "single", true, false);
        a1.setKnowledgePointId("kp-1");
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of(a1));
        when(knowledgePointService.getById("kp-1")).thenReturn(kp("kp-1", "点"));

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recs = (List<Map<String, Object>>) result.get("recommendations");
        assertTrue(recs.isEmpty());
    }

    @Test
    void buildStatsRecommendationsSkippedWhenNoWrong() {
        SubmissionAnswer a1 = answer("a1", "q-1", "single", true, true);
        a1.setKnowledgePointId("kp-1");
        SubmissionAnswer a2 = answer("a2", "q-2", "single", true, true);
        a2.setKnowledgePointId("kp-1");
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of(a1, a2));
        when(knowledgePointService.getById("kp-1")).thenReturn(kp("kp-1", "已掌握"));

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recs = (List<Map<String, Object>>) result.get("recommendations");
        assertTrue(recs.isEmpty());
    }

    @Test
    void buildStatsRecommendationsSkippedWhenMasteryHigh() {
        List<SubmissionAnswer> answers = new ArrayList<>();
        answers.add(answer("a-w", "q-w", "single", true, false));
        answers.get(0).setKnowledgePointId("kp-1");
        for (int i = 0; i < 9; i++) {
            SubmissionAnswer a = answer("a-" + i, "q-" + i, "single", true, true);
            a.setKnowledgePointId("kp-1");
            answers.add(a);
        }
        when(answerService.listByTaskNo("task-1")).thenReturn(answers);
        when(knowledgePointService.getById("kp-1")).thenReturn(kp("kp-1", "好"));

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recs = (List<Map<String, Object>>) result.get("recommendations");
        assertTrue(recs.isEmpty());
    }

    @Test
    void buildStatsRecommendationsMediumPriority() {
        List<SubmissionAnswer> answers = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            SubmissionAnswer a = answer("aw-" + i, "qw-" + i, "single", true, false);
            a.setKnowledgePointId("kp-1");
            answers.add(a);
        }
        for (int i = 0; i < 4; i++) {
            SubmissionAnswer a = answer("ar-" + i, "qr-" + i, "single", true, true);
            a.setKnowledgePointId("kp-1");
            answers.add(a);
        }
        when(answerService.listByTaskNo("task-1")).thenReturn(answers);
        when(knowledgePointService.getById("kp-1")).thenReturn(kp("kp-1", "中点"));

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recs = (List<Map<String, Object>>) result.get("recommendations");
        assertEquals(1, recs.size());
        assertEquals("medium", recs.get(0).get("priority"));
    }

    @Test
    void buildStatsNonAutoGradableNotCountedAsWrong() {
        SubmissionAnswer manual = answer("a1", "q-1", "essay", false, false);
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of(manual));

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        assertEquals(0L, result.get("wrongAnswers"));
    }

    @Test
    void buildStatsEmptyAnswersReturnsAllEmpty() {
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of());

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        assertEquals(0, asInt(result.get("totalAnswers")));
        assertEquals(0L, result.get("wrongAnswers"));
        assertNotNull(result.get("byQuestion"));
        assertNotNull(result.get("byKnowledgePoint"));
        assertNotNull(result.get("byType"));
        assertNotNull(result.get("wrongList"));
        assertNotNull(result.get("mastery"));
        assertNotNull(result.get("recommendations"));
    }

    @Test
    void buildStatsMasteryLevelNoData() {
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of());

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mastery = (List<Map<String, Object>>) result.get("mastery");
        assertTrue(mastery.isEmpty());
    }

    @Test
    void buildStatsWrongRateCalculated() {
        SubmissionAnswer a1 = answer("a1", "q-1", "single", true, false);
        SubmissionAnswer a2 = answer("a2", "q-1", "single", true, false);
        SubmissionAnswer a3 = answer("a3", "q-1", "single", true, true);
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of(a1, a2, a3));

        Map<String, Object> result = service.buildTaskWrongStats("task-1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byQuestion = (List<Map<String, Object>>) result.get("byQuestion");
        assertEquals(66.7, (Double) byQuestion.get(0).get("wrongRate"), 0.1);
    }

    @Test
    void buildTaskCourseWrongStatsFiltersNonQuizTasks() {
        LearningTask quiz = task("task-1", "CS101", "quiz");
        LearningTask video = task("task-2", "CS101", "video");
        LearningTask reading = task("task-3", "CS101", "reading");
        when(taskService.listByCourseCode("CS101")).thenReturn(List.of(quiz, video, reading));
        when(taskService.isQuizTask(quiz)).thenReturn(true);
        when(taskService.isQuizTask(video)).thenReturn(false);
        when(taskService.isQuizTask(reading)).thenReturn(false);
        when(answerService.listByTaskNo("task-1")).thenReturn(List.of(
                answer("a1", "q-1", "single", true, false)));
        when(answerService.listByTaskNo("task-2")).thenReturn(List.of());
        when(answerService.listByTaskNo("task-3")).thenReturn(List.of());

        Map<String, Object> result = service.buildCourseWrongStats("CS101");

        assertEquals(1, asInt(result.get("totalAnswers")));
    }

    // ============ helpers ============

    private static SubmissionAnswer answer(String id, String questionId, String qType,
                                          boolean autoGradable, boolean correct) {
        SubmissionAnswer a = new SubmissionAnswer();
        a.setId(id);
        a.setSubmissionId("sub-" + id);
        a.setTaskNo("task-1");
        a.setQuestionId(questionId);
        a.setQuestionType(qType);
        a.setAutoGradable(autoGradable);
        a.setCorrect(correct);
        a.setCreateTime(LocalDateTime.now());
        return a;
    }

    private static KnowledgePoint kp(String id, String name) {
        KnowledgePoint kp = new KnowledgePoint();
        kp.setKnowledgePointId(id);
        kp.setName(name);
        return kp;
    }

    private static LearningTask task(String taskNo, String courseCode, String type) {
        LearningTask t = new LearningTask();
        t.setTaskNo(taskNo);
        t.setCourseCode(courseCode);
        t.setTaskType(type);
        return t;
    }
}

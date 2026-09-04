package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.common.event.GameEventPublisher;
import com.neu.CoursePlatform.controller.BehaviorLogController;
import com.neu.CoursePlatform.controller.TaskController;
import com.neu.CoursePlatform.controller.TaskSubmissionController;
import com.neu.CoursePlatform.entity.LearningBehaviorLog;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.entity.SubmissionAnswer;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.service.impl.BehaviorLogServiceImpl;
import com.neu.CoursePlatform.service.impl.LearningTaskServiceImpl;
import com.neu.CoursePlatform.service.impl.TaskSubmissionServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class Module2ServiceTest {

    @Test
    void taskDeadlineParsingAndQuizTypeMatchModule2Contract() {
        LearningTaskServiceImpl service = new LearningTaskServiceImpl(null);
        LearningTask task = new LearningTask();

        service.applyDeadline(task, "2026-06-25 20:00:00");
        assertEquals(LocalDateTime.of(2026, 6, 25, 20, 0), task.getDeadline());

        assertThrows(IllegalArgumentException.class, () -> service.applyDeadline(task, "bad-date"));

        task.setTaskType("quiz");
        assertTrue(service.isQuizTask(task));
        task.setTaskType("在线测验");
        assertTrue(service.isQuizTask(task));
        task.setTaskType("boss_exam");
        assertTrue(service.isQuizTask(task));
        task.setTaskType("report");
        assertFalse(service.isQuizTask(task));
    }

    @Test
    void behaviorLogRecordFillsMissingTimestamps() {
        CapturingBehaviorLogService service = new CapturingBehaviorLogService();
        LearningBehaviorLog log = new LearningBehaviorLog();
        log.setUserId("student-1");
        log.setActionType("view");

        service.record(log);

        assertSame(log, service.saved);
        assertNotNull(log.getCreatedAt());
        assertNotNull(log.getStartTime());
    }

    @Test
    void behaviorLogControllerMapsSpecQueryParamsToServiceFilters() {
        CapturingBehaviorLogQuery queryService = new CapturingBehaviorLogQuery();
        BehaviorLogController controller = new BehaviorLogController(queryService, new Auth(null));
        MockHttpSession session = new MockHttpSession();
        Teacher teacher = new Teacher();
        teacher.setRole("admin");
        session.setAttribute("teacher", teacher);

        Result<List<LearningBehaviorLog>> result = controller.query("student-1", "course-1",
                "video_play", null, null, "2026-06-25T10:00:00", null,
                "2026-06-25T11:00:00", null, null, null, session);

        assertEquals(200, result.getCode());
        assertEquals("student-1", queryService.filters.get("userId"));
        assertEquals("course-1", queryService.filters.get("course_id"));
        assertEquals("video_play", queryService.filters.get("actionType"));
        assertEquals("2026-06-25T10:00:00", queryService.filters.get("startTime"));
        assertEquals("2026-06-25T11:00:00", queryService.filters.get("endTime"));
    }

    @Test
    void taskStatsCalculatesCompletionRateFromUniqueSubmittedStudents() {
        LearningTask task = task("task-1", "course-1", "homework");
        LearningTaskService taskService = proxy(LearningTaskService.class, (method, args) -> {
            if ("getById".equals(method)) return task;
            return defaultValue(method);
        });
        TaskSubmission submitted = submission("task-1", "student-1", "answer");
        submitted.setStatus("graded");
        submitted.setScore(80);
        TaskSubmission duplicate = submission("task-1", "student-1", "answer again");
        duplicate.setStatus("submitted");
        TaskSubmission superseded = submission("task-1", "student-2", "old answer");
        superseded.setStatus("superseded");
        TaskSubmissionService submissionService = proxy(TaskSubmissionService.class, (method, args) -> {
            if ("listByTaskNo".equals(method)) return List.of(submitted, duplicate, superseded);
            return defaultValue(method);
        });
        StudentService studentService = proxy(StudentService.class, (method, args) -> {
            if ("count".equals(method)) return 4L;
            return defaultValue(method);
        });
        TaskController controller = new TaskController(taskService, null, submissionService, studentService, null, new Auth(null));
        MockHttpSession session = adminSession();

        Result<Map<String, Object>> result = controller.taskStats("course-1", "task-1", session);

        assertEquals(200, result.getCode());
        assertEquals(4L, result.getData().get("totalStudents"));
        assertEquals(1L, result.getData().get("submittedStudents"));
        assertEquals(2, result.getData().get("totalSubmissions"));
        assertEquals(25.0, result.getData().get("completionRate"));
    }

    @Test
    void studentSubmissionsQueryAcceptsContractCourseIdParameter() {
        TaskSubmission courseOne = submission("task-1", "student-1", "answer");
        TaskSubmission courseTwo = submission("task-2", "student-1", "answer");
        TaskSubmissionService submissionService = proxy(TaskSubmissionService.class, (method, args) -> switch (method) {
            case "listByStudentNo" -> List.of(courseOne, courseTwo);
            case "getTaskCourseCode" -> "task-1".equals(args[0]) ? "course-1" : "course-2";
            default -> defaultValue(method);
        });
        TaskSubmissionController controller = new TaskSubmissionController(submissionService, null, null, null, new Auth(null));
        MockHttpSession session = adminSession();

        Result<List<TaskSubmission>> result = controller.listByStudent("student-1", null, "course-1", session);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("task-1", result.getData().get(0).getTaskNo());
    }

    @Test
    void teacherSubmissionQueryRequiresOwnedCourseAndStudentsOnlySeeTheirOwnRecords() {
        TaskSubmission owned = submission("task-1", "student-1", "answer");
        TaskSubmissionService submissionService = proxy(TaskSubmissionService.class, (method, args) ->
                "listByStudentNoAndCourse".equals(method) ? List.of(owned) : defaultValue(method));
        Auth auth = mock(Auth.class);
        when(auth.isAdmin(any())).thenReturn(false);
        when(auth.isTeacher(any())).thenReturn(true);
        when(auth.canModifyCourse(any(), eq("course-1"))).thenReturn(true);
        TaskSubmissionController controller = new TaskSubmissionController(submissionService, null, null, null, auth);
        MockHttpSession teacherSession = new MockHttpSession();

        Result<List<TaskSubmission>> missingCourse = controller.listByStudent("student-1", null, null, teacherSession);
        assertNotEquals(200, missingCourse.getCode());

        Result<List<TaskSubmission>> ownedCourse = controller.listByStudent("student-1", "course-1", null, teacherSession);
        assertEquals(200, ownedCourse.getCode());
        assertEquals(1, ownedCourse.getData().size());

        when(auth.canModifyCourse(any(), eq("course-2"))).thenReturn(false);
        Result<List<TaskSubmission>> foreignCourse = controller.listByStudent("student-1", "course-2", null, teacherSession);
        assertNotEquals(200, foreignCourse.getCode());

        Auth studentAuth = new Auth(null);
        TaskSubmissionController studentController = new TaskSubmissionController(
                submissionService, null, null, null, studentAuth);
        MockHttpSession studentSession = new MockHttpSession();
        Student student = new Student();
        student.setStudentNo("student-1");
        studentSession.setAttribute("student", student);
        assertEquals(200, studentController.listByStudent("student-1", "course-1", null, studentSession).getCode());
        assertNotEquals(200, studentController.listByStudent("student-2", "course-1", null, studentSession).getCode());
    }

    @Test
    void taskSubmissionInitialGradingHandlesHomeworkAndObjectiveQuiz() {
        LearningTask homework = task("task-homework", "course-1", "homework");
        TaskSubmissionServiceImpl homeworkService = submissionService(homework, Map.of(), List.of(), event -> { });
        TaskSubmission homeworkSubmission = submission("task-homework", "student-1", "plain text");

        homeworkService.applyInitialGrading(homeworkSubmission);

        assertEquals("submitted", homeworkSubmission.getStatus());
        assertEquals("待教师评阅", homeworkSubmission.getFeedback());

        LearningTask quiz = task("task-quiz", "course-1", "quiz");
        Question question = question("q1", "single", "A", 10, "kp-1");
        TaskSubmissionServiceImpl quizService = submissionService(quiz, Map.of("q1", question), List.of(), event -> { });
        TaskSubmission quizSubmission = submission("task-quiz", "student-1", "[{\"no\":\"q1\",\"response\":\"A\"}]");

        quizService.applyInitialGrading(quizSubmission);

        assertEquals("graded", quizSubmission.getStatus());
        assertEquals(10, quizSubmission.getScore());
        assertEquals("系统已自动评阅", quizSubmission.getFeedback());
    }

    @Test
    void quizGradingRejectsDuplicateAndForeignQuestionsAndCountsMissingAsWrong() {
        LearningTask quiz = task("task-quiz", "course-1", "quiz");
        quiz.setScore(15);
        Question q1 = question("q1", "single", "A", 10, "kp-1");
        Question q2 = question("q2", "single", "B", 10, "kp-1");
        TaskSubmissionServiceImpl service = submissionService(
                quiz, Map.of("q1", q1, "q2", q2), List.of(), event -> { });

        TaskSubmission missingAnswer = submission("task-quiz", "student-1",
                "[{\"no\":\"q1\",\"response\":\"A\"}]");
        service.applyInitialGrading(missingAnswer);
        assertEquals(10, missingAnswer.getScore());

        TaskSubmission duplicate = submission("task-quiz", "student-1",
                "[{\"no\":\"q1\",\"response\":\"A\"},{\"no\":\"q1\",\"response\":\"A\"}]");
        assertThrows(IllegalArgumentException.class, () -> service.applyInitialGrading(duplicate));

        TaskSubmission foreign = submission("task-quiz", "student-1",
                "[{\"no\":\"outside\",\"response\":\"A\"}]");
        assertThrows(IllegalArgumentException.class, () -> service.applyInitialGrading(foreign));

        TaskSubmission overMaximum = submission("task-quiz", "student-1",
                "[{\"no\":\"q1\",\"response\":\"A\"},{\"no\":\"q2\",\"response\":\"B\"}]");
        service.applyInitialGrading(overMaximum);
        assertEquals(15, overMaximum.getScore());
    }

    @Test
    void finalQuizScoreIsRecalculatedFromPersistedAnswerScores() {
        LearningTask quiz = task("task-quiz", "course-1", "quiz");
        quiz.setScore(12);
        SubmissionAnswer objective = new SubmissionAnswer();
        objective.setSubmissionId("submission-1");
        objective.setScore(10);
        SubmissionAnswer subjective = new SubmissionAnswer();
        subjective.setSubmissionId("submission-1");
        subjective.setScore(5);

        TaskSubmissionServiceImpl service = submissionService(quiz, Map.of(),
                List.of(objective, subjective), event -> { });
        TaskSubmission submission = submission("task-quiz", "student-1", "[]");
        submission.setSubmissionId("submission-1");

        assertEquals(12, service.recalculateFinalScore(submission));
    }

    @Test
    void assessmentResultPublishesAnswerEventsForDownstreamModules() {
        LearningTask quiz = task("task-quiz", "course-1", "quiz");
        Question question = question("q1", "single", "A", 10, "kp-1");
        SubmissionAnswer answer = new SubmissionAnswer();
        answer.setId("answer-1");
        answer.setSubmissionId("submission-1");
        answer.setTaskNo("task-quiz");
        answer.setStudentNo("student-1");
        answer.setQuestionId("q1");
        answer.setKnowledgePointId("kp-1");
        answer.setAutoGradable(true);
        answer.setCorrect(true);
        answer.setMaxScore(10);
        List<GameEvent> events = new ArrayList<>();
        TaskSubmissionServiceImpl service = submissionService(quiz, Map.of("q1", question), List.of(answer), events::add);
        TaskSubmission submission = submission("task-quiz", "student-1", "[]");
        submission.setSubmissionId("submission-1");

        service.publishAssessmentResultEvents(submission);

        assertEquals(1, events.size());
        GameEvent event = events.get(0);
        assertEquals(GameEventTypes.ANSWER_CORRECT, event.getEventType());
        assertEquals("student-1", event.getStudentId());
        assertEquals("course-1", event.getCourseId());
        assertEquals("kp-1", event.getPayload().get("knowledge_point_id"));
    }

    private static TaskSubmissionServiceImpl submissionService(LearningTask task,
                                                               Map<String, Question> questions,
                                                               List<SubmissionAnswer> answers,
                                                               GameEventPublisher publisher) {
        LearningTaskService taskService = proxy(LearningTaskService.class, (method, args) -> switch (method) {
            case "getById" -> task;
            case "isQuizTask" -> task != null && ("quiz".equalsIgnoreCase(task.getTaskType())
                    || LearningTaskService.ONLINE_QUIZ_TYPE.equals(task.getTaskType())
                    || "boss".equalsIgnoreCase(task.getTaskType())
                    || "boss_exam".equalsIgnoreCase(task.getTaskType()));
            default -> defaultValue(method);
        });
        QuestionService questionService = proxy(QuestionService.class, (method, args) -> {
            if ("getById".equals(method)) return questions.get(String.valueOf(args[0]));
            return defaultValue(method);
        });
        SubmissionAnswerService answerService = proxy(SubmissionAnswerService.class, (method, args) -> switch (method) {
            case "listBySubmissionId", "listByStudentNo" -> answers;
            case "saveBatch" -> true;
            default -> defaultValue(method);
        });
        CourseGameConfigService gameConfigService = proxy(CourseGameConfigService.class, (method, args) -> false);
        FloorProgressService floorProgressService = proxy(FloorProgressService.class, (method, args) -> null);
        StudentService studentService = proxy(StudentService.class, (method, args) -> null);
        KnowledgePointService pointService = proxy(KnowledgePointService.class, (method, args) -> null);
        com.neu.CoursePlatform.service.TaskQuestionService taskQuestionService = proxy(
                com.neu.CoursePlatform.service.TaskQuestionService.class, (method, args) -> {
                    if (!"listByTaskNo".equals(method)) return defaultValue(method);
                    return questions.keySet().stream().map(questionId -> {
                        com.neu.CoursePlatform.entity.TaskQuestion relation = new com.neu.CoursePlatform.entity.TaskQuestion();
                        relation.setTaskNo(task.getTaskNo());
                        relation.setQuestionId(questionId);
                        return relation;
                    }).toList();
                });
        return new TaskSubmissionServiceImpl(taskService, studentService, questionService, answerService,
                pointService, gameConfigService, floorProgressService, publisher, null, taskQuestionService);
    }

    private static LearningTask task(String taskNo, String courseCode, String taskType) {
        LearningTask task = new LearningTask();
        task.setTaskNo(taskNo);
        task.setCourseCode(courseCode);
        task.setTaskType(taskType);
        return task;
    }

    private static TaskSubmission submission(String taskNo, String studentNo, String content) {
        TaskSubmission submission = new TaskSubmission();
        submission.setTaskNo(taskNo);
        submission.setStudentNo(studentNo);
        submission.setContent(content);
        return submission;
    }

    private static Question question(String id, String type, String answer, Integer score, String knowledgePointId) {
        Question question = new Question();
        question.setQuestionId(id);
        question.setType(type);
        question.setAnswer(answer);
        question.setScore(score);
        question.setKnowledgePointId(knowledgePointId);
        return question;
    }

    private static MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        Teacher teacher = new Teacher();
        teacher.setRole("admin");
        session.setAttribute("teacher", teacher);
        return session;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(proxy, args);
            }
            return invocation.invoke(method.getName(), args == null ? new Object[0] : args);
        });
    }

    private static Object defaultValue(String method) {
        return switch (method) {
            case "save", "saveBatch", "updateById", "removeById" -> false;
            case "count" -> 0L;
            default -> null;
        };
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String method, Object[] args) throws Throwable;
    }

    private static class CapturingBehaviorLogService extends BehaviorLogServiceImpl {
        private LearningBehaviorLog saved;

        @Override
        public boolean save(LearningBehaviorLog entity) {
            this.saved = entity;
            return true;
        }
    }

    private static class CapturingBehaviorLogQuery extends BehaviorLogServiceImpl {
        private Map<String, String> filters;

        @Override
        public List<LearningBehaviorLog> query(Map<String, String> filters) {
            this.filters = filters;
            return List.of();
        }
    }
}

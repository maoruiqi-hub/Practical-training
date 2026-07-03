package com.neu.CoursePlatform.module5_analytics.service.external;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.dto.AssessmentMistakeStat;
import com.neu.CoursePlatform.dto.AssessmentScoreRecord;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.LearningBehaviorLog;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.module5_analytics.dto.TaskCompletionDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.KnowledgePointDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.MistakeStatsDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentProgressDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentScoreDTO;
import com.neu.CoursePlatform.module5_analytics.service.ClassInfoService;
import com.neu.CoursePlatform.service.AssessmentDataService;
import com.neu.CoursePlatform.service.BehaviorLogService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.StudentService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

class Module13ExternalDataProviderTest {

    private AssessmentDataService assessmentDataService;
    private TaskSubmissionService submissionService;
    private LearningTaskService taskService;
    private StudentService studentService;
    private KnowledgePointService knowledgePointService;
    private ClassInfoService classInfoService;
    private BehaviorLogService behaviorLogService;
    private Module13ExternalDataProvider provider;

    @BeforeEach
    void setUp() {
        assessmentDataService = mock(AssessmentDataService.class);
        submissionService = mock(TaskSubmissionService.class);
        taskService = mock(LearningTaskService.class);
        studentService = mock(StudentService.class);
        knowledgePointService = mock(KnowledgePointService.class);
        classInfoService = mock(ClassInfoService.class);
        behaviorLogService = mock(BehaviorLogService.class);
        provider = new Module13ExternalDataProvider(assessmentDataService, submissionService, taskService,
                studentService, knowledgePointService, classInfoService, behaviorLogService);
    }

    @Test
    void mapsAssessmentScoresAndMistakeStats() {
        LocalDateTime scoredAt = LocalDateTime.now();
        when(assessmentDataService.getStudentScores("s1", "c1")).thenReturn(List.of(
                new AssessmentScoreRecord("s1", "张三", "c1", "exam-1", "exam", 88, 100, scoredAt)));
        when(assessmentDataService.getClassMistakeStats("c1")).thenReturn(List.of(
                new AssessmentMistakeStat("kp1", "循环", 10, 4, 0.4)));

        StudentScoreDTO score = provider.getStudentScores("s1", "c1").get(0);
        assertEquals("张三", score.getStudentName());
        assertEquals(88, score.getScore());
        assertEquals(scoredAt, score.getScoredAt());

        MistakeStatsDTO mistake = provider.getClassMistakeStats("c1").get(0);
        assertEquals("kp1", mistake.getKnowledgePointId());
        assertEquals(0.4, mistake.getMistakeRate());
    }

    @Test
    void computesStudentAndClassProgressFromTasksAndSubmissions() {
        Student student = new Student();
        student.setName("李四");
        when(studentService.getById("s1")).thenReturn(student);
        when(taskService.listByCourseCode("c1")).thenReturn(List.of(task("t1", "任务1"), task("t2", "任务2")));
        TaskSubmission submitted = submission("s1", "t1", "submitted", 0);
        TaskSubmission superseded = submission("s1", "t2", "superseded", 0);
        when(submissionService.listByStudentNo("s1")).thenReturn(List.of(submitted, superseded));
        when(submissionService.getTaskCourseCode("t1")).thenReturn("c1");
        when(submissionService.getTaskCourseCode("t2")).thenReturn("c1");

        StudentProgressDTO progress = provider.getStudentProgress("s1", "c1");

        assertEquals("李四", progress.getStudentName());
        assertEquals(2, progress.getTotalTasks());
        assertEquals(1, progress.getSubmittedTasks());
        assertEquals(0.5, progress.getCompletionRate());

        when(classInfoService.getStudentIds("class-1")).thenReturn(List.of("s1"));
        assertEquals(1, provider.getClassProgressList("class-1", "c1").size());
    }

    @Test
    void computesTaskCompletionWithLateAndMissingStudents() {
        when(classInfoService.getStudentIds("class-1")).thenReturn(List.of("s1", "s2", "s3"));
        when(submissionService.listByTaskNo("t1")).thenReturn(List.of(
                submission("s1", "t1", "submitted", 0),
                submission("s2", "t1", "graded", 1),
                submission("s3", "t1", "superseded", 0),
                submission("outsider", "t1", "submitted", 0)));
        LearningTask task = task("t1", "");
        task.setDescription("描述名");
        when(taskService.getById("t1")).thenReturn(task);

        TaskCompletionDTO dto = provider.getTaskCompletion("class-1", "t1");

        assertEquals("描述名", dto.getTaskName());
        assertEquals(3, dto.getTotalStudents());
        assertEquals(2, dto.getSubmittedCount());
        assertEquals(1, dto.getLateSubmittedCount());
        assertEquals(List.of("s3"), dto.getNotSubmittedStudentIds());
        assertEquals(0.6667, dto.getSubmissionRate());
    }

    @Test
    void mapsKnowledgePointsStudentIdsAndLastActiveTime() {
        KnowledgePoint point = new KnowledgePoint();
        point.setKnowledgePointId("kp1");
        point.setName("函数");
        point.setImportance(null);
        when(knowledgePointService.listByCourseCode("c1", null)).thenReturn(List.of(point));
        when(classInfoService.getStudentIds("class-1")).thenReturn(List.of("s1", "s2"));
        LocalDateTime older = LocalDateTime.now().minusDays(2);
        LocalDateTime newer = LocalDateTime.now().minusHours(3);
        LearningBehaviorLog oldLog = log(older);
        LearningBehaviorLog newLog = log(newer);
        when(behaviorLogService.listByUserId("s1")).thenReturn(List.of(oldLog, newLog));

        KnowledgePointDTO dto = provider.getKnowledgePointsByCourse("c1").get(0);
        assertEquals("kp1", dto.getId());
        assertEquals(1, dto.getLevel());
        assertEquals(List.of("s1", "s2"), provider.getStudentIdsByClass("class-1"));
        assertEquals(newer, provider.getLastActiveTime("s1"));
    }

    @Test
    void handlesEmptyTaskAndUnknownStudentDefaults() {
        when(studentService.getById("s1")).thenReturn(null);
        when(taskService.listByCourseCode("c1")).thenReturn(List.of());
        when(submissionService.listByStudentNo("s1")).thenReturn(List.of());
        when(classInfoService.getStudentIds("class-empty")).thenReturn(List.of());
        when(submissionService.listByTaskNo("t404")).thenReturn(List.of());
        when(taskService.getById("t404")).thenReturn(null);
        when(behaviorLogService.listByUserId("s1")).thenReturn(List.of(log(null)));

        StudentProgressDTO progress = provider.getStudentProgress("s1", "c1");
        assertEquals("", progress.getStudentName());
        assertEquals(0, progress.getCompletionRate());

        TaskCompletionDTO completion = provider.getTaskCompletion("class-empty", "t404");
        assertEquals("任务-t404", completion.getTaskName());
        assertEquals(0, completion.getSubmissionRate());
        assertNull(provider.getLastActiveTime("s1"));
    }

    private static LearningTask task(String taskNo, String name) {
        LearningTask task = new LearningTask();
        task.setTaskNo(taskNo);
        task.setTaskName(name);
        return task;
    }

    private static TaskSubmission submission(String studentNo, String taskNo, String status, Integer overdue) {
        TaskSubmission submission = new TaskSubmission();
        submission.setStudentNo(studentNo);
        submission.setTaskNo(taskNo);
        submission.setStatus(status);
        submission.setIsOverdue(overdue);
        return submission;
    }

    private static LearningBehaviorLog log(LocalDateTime createdAt) {
        LearningBehaviorLog log = new LearningBehaviorLog();
        log.setCreatedAt(createdAt);
        return log;
    }
}

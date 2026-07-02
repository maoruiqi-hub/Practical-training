package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.service.impl.StatsServiceImpl;
import com.neu.CoursePlatform.service.TaskAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StatsServiceTest {

    private StatsServiceImpl service;
    private Map<String, LearningTask> taskStore;
    private List<TaskSubmission> subStore;

    @BeforeEach
    void setUp() {
        taskStore = new LinkedHashMap<>();
        subStore = new ArrayList<>();

        // 2 tasks for course-1
        taskStore.put("t1", task("t1", "course-1", "作业1", "homework", 100));
        taskStore.put("t2", task("t2", "course-1", "测验1", "quiz", 50));

        // Submissions: student-1 submitted both (one graded, one submitted)
        subStore.add(submission("sub-1", "t1", "student-1", "graded", 90, null));
        subStore.add(submission("sub-2", "t2", "student-1", "submitted", 0, 1)); // overdue

        LearningTaskService taskProxy = (LearningTaskService) Proxy.newProxyInstance(
                LearningTaskService.class.getClassLoader(), new Class<?>[]{LearningTaskService.class},
                (p, method, args) -> {
                    if ("getById".equals(method.getName())) return taskStore.get(String.valueOf(args[0]));
                    if ("listByCourseCode".equals(method.getName())) {
                        String cc = (String) args[0];
                        return taskStore.values().stream().filter(t -> cc.equals(t.getCourseCode())).toList();
                    }
                    return null;
                });

        TaskSubmissionService subProxy = (TaskSubmissionService) Proxy.newProxyInstance(
                TaskSubmissionService.class.getClassLoader(), new Class<?>[]{TaskSubmissionService.class},
                (p, method, args) -> {
                    if ("listByStudentNo".equals(method.getName())) {
                        String sn = (String) args[0];
                        return subStore.stream().filter(s -> sn.equals(s.getStudentNo())).toList();
                    }
                    if ("listByTaskNo".equals(method.getName())) {
                        String tn = (String) args[0];
                        return subStore.stream().filter(s -> tn.equals(s.getTaskNo())).toList();
                    }
                    return null;
                });

        BehaviorLogService logProxy = (BehaviorLogService) Proxy.newProxyInstance(
                BehaviorLogService.class.getClassLoader(), new Class<?>[]{BehaviorLogService.class},
                (p, method, args) -> {
                    if ("listByUserId".equals(method.getName())) return List.of();
                    return null;
                });

        TaskAssignmentService assignmentProxy = (TaskAssignmentService) Proxy.newProxyInstance(
                TaskAssignmentService.class.getClassLoader(), new Class<?>[]{TaskAssignmentService.class},
                (p, method, args) -> List.of());

        service = new StatsServiceImpl(subProxy, taskProxy, assignmentProxy, logProxy);
    }

    @Test
    void buildStudentStats() {
        Map<String, Object> stats = service.buildStudentStats("student-1");
        assertEquals("student-1", stats.get("studentNo"));
        assertEquals(2, stats.get("totalSubmissions"));
        assertEquals(2, stats.get("completedCount"));
        assertEquals(1, stats.get("gradedCount"));
        assertEquals(1, stats.get("overdueCount"));
        assertEquals(90.0, stats.get("averageScore"));
    }

    @Test
    void buildStudentCourseStats() {
        Map<String, Object> stats = service.buildStudentCourseStats("student-1", "course-1");
        assertEquals(2, stats.get("totalTasks"));
        assertEquals(2, stats.get("completedCount"));
        assertEquals(100L, stats.get("completionRate"));
        assertEquals(90.0, stats.get("averageScore"));
        assertEquals(1, stats.get("overdueCount"));
    }

    @Test
    void buildCourseStats() {
        Map<String, Object> stats = service.buildCourseStats("course-1");
        assertEquals("course-1", stats.get("courseCode"));
        assertEquals(2, stats.get("taskCount"));
        assertEquals(2, stats.get("totalSubmissions"));
        assertNotNull(stats.get("taskStats"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> taskStats = (List<Map<String, Object>>) stats.get("taskStats");
        assertEquals(2, taskStats.size());
    }

    @Test
    void averageScoreIsZeroWhenNoGraded() {
        subStore.clear();
        subStore.add(submission("sub-3", "t1", "student-1", "submitted", 0, null));
        Map<String, Object> stats = service.buildStudentStats("student-1");
        assertEquals(0.0, stats.get("averageScore"));
    }

    @Test
    void completionRateIsZeroWhenNoTasks() {
        Map<String, Object> stats = service.buildStudentCourseStats("student-1", "course-999");
        assertEquals(0, stats.get("totalTasks"));
        assertEquals(0L, stats.get("completionRate"));
    }

    // ============ helpers ============

    private LearningTask task(String no, String cc, String name, String type, int score) {
        LearningTask t = new LearningTask();
        t.setTaskNo(no);
        t.setCourseCode(cc);
        t.setTaskName(name);
        t.setTaskType(type);
        t.setScore(score);
        return t;
    }

    private TaskSubmission submission(String id, String taskNo, String studentNo,
                                       String status, Integer score, Integer isOverdue) {
        TaskSubmission s = new TaskSubmission();
        s.setSubmissionId(id);
        s.setTaskNo(taskNo);
        s.setStudentNo(studentNo);
        s.setStatus(status);
        s.setScore(score);
        s.setIsOverdue(isOverdue);
        s.setSubmitTime(LocalDateTime.now());
        return s;
    }
}

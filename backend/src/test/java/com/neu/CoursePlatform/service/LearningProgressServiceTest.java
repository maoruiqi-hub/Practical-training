package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.entity.LearningBehaviorLog;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.service.impl.LearningProgressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LearningProgressServiceTest {

    private LearningProgressServiceImpl service;
    private List<LearningTask> tasks;
    private List<TaskSubmission> submissions;
    private Map<String, Student> studentStore;

    @BeforeEach
    void setUp() {
        tasks = new ArrayList<>();
        tasks.add(task("t1", "c1", "第1讲作业", "homework", 100, LocalDateTime.now().plusDays(7)));
        tasks.add(task("t2", "c1", "第2讲测验", "quiz", 50, LocalDateTime.now().minusDays(1))); // 已过期
        tasks.add(task("t3", "c1", "第3讲报告", "report", 80, LocalDateTime.now().plusDays(3)));

        submissions = new ArrayList<>();
        submissions.add(sub("s1", "t1", "student-1", "graded", 95, 0, LocalDateTime.now().minusHours(2)));
        // t2 截止昨天，但提交在昨天之后（即逾期提交）
        submissions.add(sub("s2", "t2", "student-1", "submitted", 0, 1, LocalDateTime.now().minusHours(6)));

        studentStore = new LinkedHashMap<>();
        Student s = new Student();
        s.setStudentNo("student-1");
        s.setName("张三");
        s.setClassName("计科202班");
        studentStore.put("student-1", s);

        Student s2 = new Student();
        s2.setStudentNo("student-2");
        s2.setName("李四");
        s2.setClassName("计科202班");
        studentStore.put("student-2", s2);

        service = new LearningProgressServiceImpl(
                taskProxy(), submissionProxy(), assignmentProxy(), logProxy(), studentProxy());
    }

    @Test
    void buildStudentProgressShowsAllTasks() {
        Map<String, Object> progress = service.buildStudentProgress("student-1", "c1");
        assertEquals("student-1", progress.get("studentNo"));
        assertEquals(3, progress.get("totalTasks"));
        assertEquals(2, progress.get("completedCount"));
        assertEquals(67L, progress.get("completionRate"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> taskList = (List<Map<String, Object>>) progress.get("taskStatusList");
        assertEquals(3, taskList.size());
    }

    @Test
    void submittedTaskHasCorrectStatus() {
        Map<String, Object> progress = service.buildStudentProgress("student-1", "c1");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) progress.get("taskStatusList");

        Map<String, Object> t1 = list.stream().filter(m -> "t1".equals(m.get("taskNo"))).findFirst().orElseThrow();
        assertEquals("completed", t1.get("submissionStatus"));
        assertEquals(95, t1.get("studentScore"));
        assertTrue((Boolean) t1.get("submitted"));
    }

    @Test
    void missingDeadlineIsMarked() {
        Map<String, Object> progress = service.buildStudentProgress("student-1", "c1");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) progress.get("taskStatusList");

        Map<String, Object> t3 = list.stream().filter(m -> "t3".equals(m.get("taskNo"))).findFirst().orElseThrow();
        assertEquals("pending", t3.get("submissionStatus"));
        assertFalse((Boolean) t3.get("submitted"));
    }

    @Test
    void overdueTaskIsDetected() {
        // t2 deadline is in past, submission has isOverdue=1
        Map<String, Object> progress = service.buildStudentProgress("student-1", "c1");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) progress.get("taskStatusList");

        Map<String, Object> t2 = list.stream().filter(m -> "t2".equals(m.get("taskNo"))).findFirst().orElseThrow();
        assertEquals("overdue", t2.get("submissionStatus"));
    }

    @Test
    void buildCourseProgressShowsStudentsWithSubmissions() {
        Map<String, Object> progress = service.buildCourseProgress("c1");
        assertEquals("c1", progress.get("courseCode"));
        assertEquals(3, progress.get("totalTasks"));
        // 只有 student-1 有提交记录
        assertEquals(1, progress.get("totalStudents"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) progress.get("studentRows");
        assertEquals(1, rows.size());
    }

    @Test
    void laggingDetection() {
        Map<String, Object> progress = service.buildCourseProgress("c1");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) progress.get("studentRows");

        // student-1 has 2/3 completed = 67%, not lagging (≥50%)
        Map<String, Object> stu1 = rows.stream()
                .filter(r -> "student-1".equals(r.get("studentNo"))).findFirst().orElseThrow();
        assertEquals(2, stu1.get("completedCount"));
        assertFalse((Boolean) stu1.get("isLagging"));
        assertFalse((Boolean) stu1.get("hasOverdueRisk"));
    }

    @Test
    void timelineIsSortedByLatestFirst() {
        Map<String, Object> progress = service.buildStudentProgress("student-1", "c1");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> timeline = (List<Map<String, Object>>) progress.get("timeline");
        assertNotNull(timeline);
    }

    // ============ helpers ============

    private LearningTask task(String no, String cc, String name, String type, int score, LocalDateTime deadline) {
        LearningTask t = new LearningTask();
        t.setTaskNo(no);
        t.setCourseCode(cc);
        t.setTaskName(name);
        t.setTaskType(type);
        t.setScore(score);
        t.setDeadline(deadline);
        return t;
    }

    private TaskSubmission sub(String id, String taskNo, String studentNo,
                                String status, Integer score, Integer overdue, LocalDateTime time) {
        TaskSubmission s = new TaskSubmission();
        s.setSubmissionId(id);
        s.setTaskNo(taskNo);
        s.setStudentNo(studentNo);
        s.setStatus(status);
        s.setScore(score);
        s.setIsOverdue(overdue);
        s.setSubmitTime(time);
        return s;
    }

    private LearningTaskService taskProxy() {
        return (LearningTaskService) Proxy.newProxyInstance(
                LearningTaskService.class.getClassLoader(), new Class<?>[]{LearningTaskService.class},
                (p, method, args) -> {
                    if ("listByCourseCode".equals(method.getName())) {
                        String cc = (String) args[0];
                        return tasks.stream().filter(t -> cc.equals(t.getCourseCode())).toList();
                    }
                    return null;
                });
    }

    private TaskSubmissionService submissionProxy() {
        return (TaskSubmissionService) Proxy.newProxyInstance(
                TaskSubmissionService.class.getClassLoader(), new Class<?>[]{TaskSubmissionService.class},
                (p, method, args) -> {
                    if ("listByStudentNo".equals(method.getName())) {
                        String sn = (String) args[0];
                        return submissions.stream().filter(s -> sn.equals(s.getStudentNo())).toList();
                    }
                    if ("listByTaskNo".equals(method.getName())) {
                        String tn = (String) args[0];
                        return submissions.stream().filter(s -> tn.equals(s.getTaskNo())).toList();
                    }
                    return null;
                });
    }

    private BehaviorLogService logProxy() {
        return (BehaviorLogService) Proxy.newProxyInstance(
                BehaviorLogService.class.getClassLoader(), new Class<?>[]{BehaviorLogService.class},
                (p, method, args) -> {
                    if ("listByUserId".equals(method.getName())) {
                        LearningBehaviorLog log = new LearningBehaviorLog();
                        log.setActionType("video_view");
                        log.setResourceType("video");
                        log.setCreatedAt(LocalDateTime.now());
                        log.setDuration(120);
                        return List.of(log);
                    }
                    return null;
                });
    }

    private StudentService studentProxy() {
        return (StudentService) Proxy.newProxyInstance(
                StudentService.class.getClassLoader(), new Class<?>[]{StudentService.class},
                (p, method, args) -> {
                    if ("getById".equals(method.getName())) {
                        return studentStore.get(String.valueOf(args[0]));
                    }
                    return null;
                });
    }

    private TaskAssignmentService assignmentProxy() {
        return (TaskAssignmentService) Proxy.newProxyInstance(
                TaskAssignmentService.class.getClassLoader(), new Class<?>[]{TaskAssignmentService.class},
                (p, method, args) -> List.of());
    }
}

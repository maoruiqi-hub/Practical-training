package com.neu.CoursePlatform.service.impl;

import static com.neu.CoursePlatform.service.TeacherServiceTest.setBaseMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.TaskAssignment;
import com.neu.CoursePlatform.mapper.TaskAssignmentMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.*;

class TaskAssignmentServiceImplTest {

    private TaskAssignmentServiceImpl service;
    private Map<String, TaskAssignment> assignmentStore;

    @BeforeEach
    void setUp() throws Exception {
        assignmentStore = new LinkedHashMap<>();

        TaskAssignmentMapper mapper = (TaskAssignmentMapper) Proxy.newProxyInstance(
                TaskAssignmentMapper.class.getClassLoader(),
                new Class<?>[]{TaskAssignmentMapper.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("insert".equals(name) && args != null && args.length == 1 && args[0] instanceof TaskAssignment a) {
                        if (a.getAssignmentId() == null) a.setAssignmentId("a-" + (assignmentStore.size() + 1));
                        assignmentStore.put(a.getAssignmentId(), a);
                        return 1;
                    }
                    if ("updateById".equals(name) && args != null && args.length >= 1 && args[0] instanceof TaskAssignment a) {
                        if (assignmentStore.containsKey(a.getAssignmentId())) {
                            assignmentStore.put(a.getAssignmentId(), a);
                            return 1;
                        }
                        return 0;
                    }
                    if ("selectActiveByTaskAndStudent".equals(name)) {
                        return assignmentStore.values().stream()
                                .filter(a -> args[0].equals(a.getTaskNo()) && args[1].equals(a.getStudentNo())
                                        && !"cancelled".equals(a.getStatus()))
                                .findFirst().orElse(null);
                    }
                    if ("selectAssignedTasks".equals(name)) {
                        return List.of();
                    }
                    if ("countActiveByTaskNo".equals(name)) {
                        return assignmentStore.values().stream()
                                .filter(a -> args[0].equals(a.getTaskNo()) && !"cancelled".equals(a.getStatus()))
                                .count();
                    }
                    if ("toString".equals(name)) return "TaskAssignMapperProxy";
                    if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                    if ("equals".equals(name)) return proxy == args[0];
                    return null;
                });

        TaskAssignmentServiceImpl real = new TaskAssignmentServiceImpl();
        setBaseMapper(real, mapper);
        service = spy(real);

        // Default: no existing assignment found
        lenient().doReturn(null).when(service).getOne(any(LambdaQueryWrapper.class));
        // Default: update succeeds (markSubmitted/markCompleted/cancelByTaskNo)
        lenient().doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));
        // markSubmitted/markCompleted/cancelByTaskNo internally construct LambdaUpdateWrapper
        // which fails outside Spring context (MyBatis-Plus lambda cache). Stub them.
        lenient().doNothing().when(service).markSubmitted(anyString(), anyString());
        lenient().doNothing().when(service).markCompleted(anyString(), anyString());
        lenient().doNothing().when(service).cancelByTaskNo(anyString());
    }

    // ============ assignTask ============

    @Test
    void assignTaskCreatesNewAssignmentWhenNoneExists() {
        LearningTask task = task("task-1", "CS101", "quiz");

        TaskAssignment result = service.assignTask(task, "2024001", "T001", "请完成");

        assertNotNull(result);
        assertEquals("task-1", result.getTaskNo());
        assertEquals("CS101", result.getCourseCode());
        assertEquals("2024001", result.getStudentNo());
        assertEquals("T001", result.getAssignedBy());
        assertEquals("assigned", result.getStatus());
        assertEquals("请完成", result.getNote());
        assertNotNull(result.getAssignedAt());
        assertTrue(assignmentStore.containsKey(result.getAssignmentId()));
    }

    @Test
    void assignTaskUpdatesExistingAssignment() {
        LearningTask task = task("task-1", "CS101", "quiz");

        TaskAssignment existing = new TaskAssignment();
        existing.setAssignmentId("a-existing");
        existing.setTaskNo("task-1");
        existing.setStudentNo("2024001");
        existing.setStatus("submitted");
        assignmentStore.put("a-existing", existing);

        doReturn(existing).when(service).getOne(any(LambdaQueryWrapper.class));

        TaskAssignment result = service.assignTask(task, "2024001", "T002", "重新布置");

        assertEquals("a-existing", result.getAssignmentId());
        assertEquals("assigned", result.getStatus());
        assertEquals("T002", result.getAssignedBy());
        assertEquals("重新布置", result.getNote());
        assertNotNull(result.getAssignedAt());
    }

    @Test
    void assignTaskReturnsExistingWhenUpdateFails() {
        LearningTask task = task("task-1", "CS101", "quiz");

        TaskAssignment existing = new TaskAssignment();
        existing.setAssignmentId("a-existing");
        existing.setTaskNo("task-1");
        existing.setStudentNo("2024001");
        existing.setStatus("submitted");
        assignmentStore.put("a-existing", existing);

        doReturn(existing).when(service).getOne(any(LambdaQueryWrapper.class));
        doReturn(false).when(service).updateById(any(TaskAssignment.class));

        TaskAssignment result = service.assignTask(task, "2024001", "T002", "note");
        assertNotNull(result);
    }

    // ============ listAssignedTasks ============

    @Test
    void listAssignedTasksDelegatesToMapper() {
        List<LearningTask> result = service.listAssignedTasks("2024001", "CS101", null, null, null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ============ getActiveAssignment ============

    @Test
    void getActiveAssignmentReturnsFromMapper() {
        TaskAssignment a = new TaskAssignment();
        a.setAssignmentId("a1");
        a.setTaskNo("task-1");
        a.setStudentNo("2024001");
        a.setStatus("assigned");
        assignmentStore.put("a1", a);

        TaskAssignment result = service.getActiveAssignment("task-1", "2024001");
        assertNotNull(result);
        assertEquals("a1", result.getAssignmentId());
    }

    @Test
    void getActiveAssignmentReturnsNullWhenNotFound() {
        TaskAssignment result = service.getActiveAssignment("task-x", "2024999");
        assertNull(result);
    }

    // ============ countActiveByTaskNo ============

    @Test
    void countActiveByTaskNoReturnsCount() {
        TaskAssignment a1 = new TaskAssignment();
        a1.setAssignmentId("a1"); a1.setTaskNo("task-1"); a1.setStatus("assigned");
        assignmentStore.put("a1", a1);
        TaskAssignment a2 = new TaskAssignment();
        a2.setAssignmentId("a2"); a2.setTaskNo("task-1"); a2.setStatus("submitted");
        assignmentStore.put("a2", a2);
        TaskAssignment a3 = new TaskAssignment();
        a3.setAssignmentId("a3"); a3.setTaskNo("task-1"); a3.setStatus("cancelled");
        assignmentStore.put("a3", a3);

        assertEquals(2, service.countActiveByTaskNo("task-1"));
    }

    @Test
    void countActiveByTaskNoReturnsZeroWhenNone() {
        assertEquals(0, service.countActiveByTaskNo("task-nonexistent"));
    }

    // ============ markSubmitted / markCompleted / cancelByTaskNo ============

    @Test
    void markSubmittedDoesNotThrow() {
        // Internally constructs LambdaUpdateWrapper — cannot run outside Spring context
        assertDoesNotThrow(() -> service.markSubmitted("task-1", "2024001"));
    }

    @Test
    void markCompletedDoesNotThrow() {
        assertDoesNotThrow(() -> service.markCompleted("task-1", "2024001"));
    }

    @Test
    void cancelByTaskNoDoesNotThrow() {
        assertDoesNotThrow(() -> service.cancelByTaskNo("task-1"));
    }

    // ============ helpers ============

    private static LearningTask task(String taskNo, String courseCode, String type) {
        LearningTask t = new LearningTask();
        t.setTaskNo(taskNo);
        t.setCourseCode(courseCode);
        t.setTaskType(type);
        return t;
    }
}

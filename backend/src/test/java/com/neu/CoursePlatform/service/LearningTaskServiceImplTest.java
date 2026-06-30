package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.mapper.LearningTaskMapper;
import com.neu.CoursePlatform.mapper.TaskSubmissionMapper;
import com.neu.CoursePlatform.service.impl.LearningTaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LearningTaskServiceImplTest {

    private LearningTaskServiceImpl service;
    private Map<String, LearningTask> taskStore;
    private Long selectCountResult; // preset return value for selectCount proxy

    @BeforeEach
    void setUp() throws Exception {
        taskStore = new LinkedHashMap<>();
        selectCountResult = 0L;

        // Dynamic proxy for LearningTaskMapper
        LearningTaskMapper taskMapperProxy = (LearningTaskMapper) Proxy.newProxyInstance(
                LearningTaskMapper.class.getClassLoader(),
                new Class<?>[]{LearningTaskMapper.class},
                (p, method, args) -> taskMapperInvoke(taskStore, method.getName(), args));

        // Dynamic proxy for TaskSubmissionMapper
        TaskSubmissionMapper submissionMapperProxy = (TaskSubmissionMapper) Proxy.newProxyInstance(
                TaskSubmissionMapper.class.getClassLoader(),
                new Class<?>[]{TaskSubmissionMapper.class},
                (p, method, args) -> submissionMapperInvoke(this, method.getName(), args));

        service = new LearningTaskServiceImpl(submissionMapperProxy);
        setBaseMapper(service, taskMapperProxy);
    }

    // ======================== listByCourseCode ========================

    @Test
    void listByCourseCode_ReturnsFilteredTasks() {
        saveTask("task01", "C001", "作业一", "homework");
        saveTask("task02", "C001", "作业二", "homework");
        saveTask("task03", "C002", "实验一", "experiment");

        List<LearningTask> result = service.listByCourseCode("C001");

        assertEquals(2, result.size());
    }

    @Test
    void listByCourseCode_NoMatch_ReturnsEmpty() {
        List<LearningTask> result = service.listByCourseCode("NONEXISTENT");

        assertTrue(result.isEmpty());
    }

    // ======================== searchByKeyword ========================

    @Test
    void searchByKeyword_FindsByName() {
        saveTask("t1", "C001", "Python数据分析", "homework");
        saveTask("t2", "C001", "Java编程", "homework");

        List<LearningTask> result = service.searchByKeyword("Python");

        assertEquals(1, result.size());
        assertEquals("Python数据分析", result.get(0).getTaskName());
    }

    // ======================== listFiltered ========================

    @Test
    void listFiltered_WithTypeAndCourse() {
        saveTask("t1", "C001", "Task A", "homework");
        saveTask("t2", "C001", "Task B", "quiz");

        Map<String, String> filters = Map.of("courseCode", "C001", "taskType", "homework");
        List<LearningTask> result = service.listFiltered(filters);

        assertEquals(1, result.size());
    }

    // ======================== applyDeadline ========================

    @Test
    void applyDeadline_ValidFormat_SetsDeadline() {
        LearningTask task = new LearningTask();
        service.applyDeadline(task, "2026-07-15 23:59:59");

        assertNotNull(task.getDeadline());
    }

    @Test
    void applyDeadline_ValidFormatWithoutTime() {
        LearningTask task = new LearningTask();
        service.applyDeadline(task, "2026-07-15 00:00:00");

        assertNotNull(task.getDeadline());
    }

    @Test
    void applyDeadline_Null_KeepsDeadlineNull() {
        LearningTask task = new LearningTask();
        service.applyDeadline(task, null);

        assertNull(task.getDeadline());
    }

    @Test
    void applyDeadline_Empty_KeepsDeadlineNull() {
        LearningTask task = new LearningTask();
        service.applyDeadline(task, "");

        assertNull(task.getDeadline());
    }

    @Test
    void applyDeadline_InvalidFormat_ThrowsException() {
        LearningTask task = new LearningTask();

        assertThrows(IllegalArgumentException.class,
                () -> service.applyDeadline(task, "2026/07/15"));

        assertEquals("截止时间格式错误，请使用 yyyy-MM-dd HH:mm:ss",
                assertThrows(IllegalArgumentException.class,
                        () -> service.applyDeadline(task, "invalid-date")).getMessage());
    }

    @Test
    void applyDeadline_OnlyDateNoTime_ThrowsException() {
        LearningTask task = new LearningTask();

        assertThrows(IllegalArgumentException.class,
                () -> service.applyDeadline(task, "2026-07-15"));
    }

    // ======================== hasSubmissions ========================

    @Test
    void hasSubmissions_HasSubmissions() {
        selectCountResult = 3L;

        assertTrue(service.hasSubmissions("task01"));
    }

    @Test
    void hasSubmissions_NoSubmissions() {
        selectCountResult = 0L;

        assertFalse(service.hasSubmissions("task01"));
    }

    // ======================== isQuizTask ========================

    @Test
    void isQuizTask_QuizType() {
        LearningTask task = new LearningTask();
        task.setTaskType("quiz");

        assertTrue(service.isQuizTask(task));
    }

    @Test
    void isQuizTask_QuizTypeCaseInsensitive() {
        LearningTask task = new LearningTask();
        task.setTaskType("QUIZ");

        assertTrue(service.isQuizTask(task));
    }

    @Test
    void isQuizTask_HomeworkType() {
        LearningTask task = new LearningTask();
        task.setTaskType("homework");

        assertFalse(service.isQuizTask(task));
    }

    @Test
    void isQuizTask_NullTask() {
        assertFalse(service.isQuizTask(null));
    }

    @Test
    void isQuizTask_NullTaskType() {
        LearningTask task = new LearningTask();
        assertFalse(service.isQuizTask(task));
    }

    // ======================== helpers ========================

    private void saveTask(String taskNo, String courseCode, String name, String type) {
        LearningTask t = new LearningTask();
        t.setTaskNo(taskNo);
        t.setCourseCode(courseCode);
        t.setTaskName(name);
        t.setTaskType(type);
        taskStore.put(taskNo, t);
    }

    static Object taskMapperInvoke(Map<String, LearningTask> store, String methodName, Object[] args) {
        switch (methodName) {
            case "selectByCourseCode":
                return store.values().stream()
                        .filter(t -> args[0].equals(t.getCourseCode()))
                        .toList();
            case "selectByKeyword":
                String keyword = (String) args[0];
                return store.values().stream()
                        .filter(t -> t.getTaskName() != null && t.getTaskName().contains(keyword))
                        .toList();
            case "selectFiltered":
                return store.values().stream()
                        .filter(t -> args[0] == null || args[0].equals(t.getCourseCode()))
                        .filter(t -> args[1] == null || args[1].equals(t.getTaskType()))
                        .toList();
            case "insert":
                LearningTask t = (LearningTask) args[0];
                store.put(t.getTaskNo(), t);
                return 1;
            case "selectById":
                return store.get(String.valueOf(args[0]));
            default:
                return null;
        }
    }

    static Object submissionMapperInvoke(LearningTaskServiceImplTest self, String methodName, Object[] args) {
        switch (methodName) {
            case "selectCount":
                return self.selectCountResult;
            default:
                return null;
        }
    }

    static void setBaseMapper(Object service, Object mapper) throws Exception {
        Class<?> clazz = service.getClass();
        while (clazz != null) {
            try {
                java.lang.reflect.Field f = clazz.getDeclaredField("baseMapper");
                f.setAccessible(true);
                f.set(service, mapper);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
    }
}

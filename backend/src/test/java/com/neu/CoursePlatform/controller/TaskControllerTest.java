package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.TaskUpdateRequest;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.TaskAssignment;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.service.FileStorageService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.StudentService;
import com.neu.CoursePlatform.service.TaskAssignmentService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock private LearningTaskService taskService;
    @Mock private FileStorageService fileStorageService;
    @Mock private TaskSubmissionService submissionService;
    @Mock private StudentService studentService;
    @Mock private TaskAssignmentService assignmentService;
    @Mock private Auth auth;
    @Mock private HttpSession session;
    @InjectMocks private TaskController controller;

    // ============ list ============

    @Test
    void listRequiresLogin() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<List<LearningTask>> result = controller.list("CS101", null, null, null, null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void listReturnsAssignedForStudent() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        Student student = new Student();
        student.setStudentNo("2024001");
        when(auth.getStudent(session)).thenReturn(student);
        when(assignmentService.listAssignedTasks("2024001", "CS101", null, null, null))
                .thenReturn(List.of(new LearningTask()));

        Result<List<LearningTask>> result = controller.list("CS101", null, null, null, null, session);

        assertEquals(200, result.getCode());
    }

    @Test
    void listReturnsAssignedForSpecificStudent() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(auth.getStudent(session)).thenReturn(null);
        when(assignmentService.listAssignedTasks("2024002", "CS101", "homework", null, null))
                .thenReturn(List.of());

        Result<List<LearningTask>> result = controller.list("CS101", "2024002", "homework", null, null, session);

        assertEquals(200, result.getCode());
    }

    @Test
    void listReturnsFilteredTasks() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(auth.getStudent(session)).thenReturn(null);
        when(taskService.listFiltered(anyMap())).thenReturn(List.of(new LearningTask()));

        Result<List<LearningTask>> result = controller.list("CS101", null, "homework", "published", null, session);

        assertEquals(200, result.getCode());
    }

    @Test
    void listByContractDelegatesToList() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(auth.getStudent(session)).thenReturn(null);
        when(taskService.listFiltered(anyMap())).thenReturn(List.of());

        Result<List<LearningTask>> result = controller.listByContract("CS101", null, null, null, null, session);

        assertEquals(200, result.getCode());
    }

    // ============ detail ============

    @Test
    void detailRequiresLogin() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<LearningTask> result = controller.detail("task-1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void detailTaskNotFound() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(taskService.getById("task-1")).thenReturn(null);

        Result<LearningTask> result = controller.detail("task-1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void detailStudentNotAssigned() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        LearningTask task = new LearningTask();
        task.setTaskNo("task-1");
        when(taskService.getById("task-1")).thenReturn(task);
        Student student = new Student();
        student.setStudentNo("2024001");
        when(auth.getStudent(session)).thenReturn(student);
        when(assignmentService.getActiveAssignment("task-1", "2024001")).thenReturn(null);

        Result<LearningTask> result = controller.detail("task-1", session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("未分配"));
    }

    @Test
    void detailReturnsTask() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        LearningTask task = new LearningTask();
        task.setTaskNo("task-1");
        task.setTaskName("Homework1");
        when(taskService.getById("task-1")).thenReturn(task);

        Result<LearningTask> result = controller.detail("task-1", session);

        assertEquals(200, result.getCode());
    }

    // ============ search ============

    @Test
    void searchRequiresLogin() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<List<LearningTask>> result = controller.search("keyword", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void searchReturnsResults() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(taskService.searchByKeyword("Java")).thenReturn(List.of(new LearningTask()));

        Result<List<LearningTask>> result = controller.search("Java", session);

        assertEquals(200, result.getCode());
    }

    // ============ add ============

    @Test
    void addRequiresPermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<String> result = controller.add("CS101", null, null, null, "homework", "desc",
                null, "", 10, null, "published", 0, 3, null, null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void addSuccess() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<String> result = controller.add("CS101", "Task1", null, null, "homework", "desc",
                null, "", 10, null, "published", 0, 3, null, null, session);

        ArgumentCaptor<LearningTask> captor = ArgumentCaptor.forClass(LearningTask.class);
        verify(taskService).save(captor.capture());
        LearningTask saved = captor.getValue();
        assertEquals(200, result.getCode());
        assertEquals("CS101", saved.getCourseCode());
        assertEquals("Task1", saved.getTaskName());
        assertEquals("homework", saved.getTaskType());
    }

    @Test
    void addTaskNameFallsBackToDescription() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<String> result = controller.add("CS101", null, null, null, "homework", "My Description",
                null, "", 10, null, "published", 0, 3, null, null, session);

        ArgumentCaptor<LearningTask> captor = ArgumentCaptor.forClass(LearningTask.class);
        verify(taskService).save(captor.capture());
        assertEquals("My Description", captor.getValue().getTaskName());
    }

    @Test
    void addBlankTaskNameFallsBackToDescription() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<String> result = controller.add("CS101", "  ", null, null, "homework", "Desc",
                null, "", 10, null, "published", 0, 3, null, null, session);

        ArgumentCaptor<LearningTask> captor = ArgumentCaptor.forClass(LearningTask.class);
        verify(taskService).save(captor.capture());
        assertEquals("Desc", captor.getValue().getTaskName());
    }

    @Test
    void addInvalidDeadline() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        doThrow(new IllegalArgumentException("日期格式错误")).when(taskService).applyDeadline(any(), eq("invalid"));

        Result<String> result = controller.add("CS101", "Task1", null, null, "homework", "desc",
                "invalid", "", 10, null, "published", 0, 3, null, null, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("日期格式错误"));
    }

    @Test
    void addWithFile() throws IOException {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(fileStorageService.store(any(), anyString())).thenReturn("resource.pdf");
        MockMultipartFile file = new MockMultipartFile("file", "resource.pdf", "application/pdf", new byte[]{1});

        Result<String> result = controller.add("CS101", "Task1", null, null, "homework", "desc",
                null, "", 10, null, "published", 0, 3, null, file, session);

        assertEquals(200, result.getCode());
        ArgumentCaptor<LearningTask> captor = ArgumentCaptor.forClass(LearningTask.class);
        verify(taskService).save(captor.capture());
        assertEquals("resource.pdf", captor.getValue().getResourceUrl());
    }

    @Test
    void addFileUploadFails() throws IOException {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(fileStorageService.store(any(), anyString())).thenThrow(new IOException("error"));
        MockMultipartFile file = new MockMultipartFile("file", "resource.pdf", "application/pdf", new byte[]{1});

        Result<String> result = controller.add("CS101", "Task1", null, null, "homework", "desc",
                null, "", 10, null, "published", 0, 3, null, file, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("上传失败"));
    }

    // ============ update ============

    @Test
    void updateRequiresPermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<String> result = controller.update("CS101", "task-1", new TaskUpdateRequest(), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void updateTaskNotFound() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(taskService.getById("task-1")).thenReturn(null);

        Result<String> result = controller.update("CS101", "task-1", new TaskUpdateRequest(), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void updateSuccess() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        LearningTask task = new LearningTask();
        task.setTaskNo("task-1");
        when(taskService.getById("task-1")).thenReturn(task);
        TaskUpdateRequest req = new TaskUpdateRequest();
        req.setTaskName("Updated");
        req.setScore(20);

        Result<String> result = controller.update("CS101", "task-1", req, session);

        assertEquals(200, result.getCode());
        assertEquals("Updated", task.getTaskName());
        assertEquals(20, task.getScore());
        verify(taskService).updateById(task);
    }

    @Test
    void updateNullFieldsNotChanged() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        LearningTask task = new LearningTask();
        task.setTaskName("Original");
        task.setScore(10);
        when(taskService.getById("task-1")).thenReturn(task);
        TaskUpdateRequest req = new TaskUpdateRequest();

        Result<String> result = controller.update("CS101", "task-1", req, session);

        assertEquals(200, result.getCode());
        assertEquals("Original", task.getTaskName());
        assertEquals(10, task.getScore());
    }

    @Test
    void updateInvalidDeadline() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        LearningTask task = new LearningTask();
        when(taskService.getById("task-1")).thenReturn(task);
        doThrow(new IllegalArgumentException("日期格式错误")).when(taskService).applyDeadline(task, "bad-date");
        TaskUpdateRequest req = new TaskUpdateRequest();
        req.setDeadline("bad-date");

        Result<String> result = controller.update("CS101", "task-1", req, session);

        assertEquals(500, result.getCode());
    }

    // ============ updateByContract ============

    @Test
    void updateByContractTaskNotFound() {
        when(taskService.getById("task-1")).thenReturn(null);

        Result<String> result = controller.updateByContract("task-1", new TaskUpdateRequest(), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void updateByContractSuccess() {
        LearningTask task = new LearningTask();
        task.setTaskNo("task-1");
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<String> result = controller.updateByContract("task-1", new TaskUpdateRequest(), session);

        assertEquals(200, result.getCode());
        verify(taskService).updateById(task);
    }

    // ============ toggleStatus ============

    @Test
    void toggleStatusRequiresPermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<String> result = controller.toggleStatus("CS101", "task-1", Map.of("status", "published"), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void toggleStatusTaskNotFound() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(taskService.getById("task-1")).thenReturn(null);

        Result<String> result = controller.toggleStatus("CS101", "task-1", Map.of("status", "published"), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void toggleStatusNullStatus() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(taskService.getById("task-1")).thenReturn(new LearningTask());

        Result<String> result = controller.toggleStatus("CS101", "task-1", Map.of(), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void toggleStatusInvalidStatus() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(taskService.getById("task-1")).thenReturn(new LearningTask());

        Result<String> result = controller.toggleStatus("CS101", "task-1", Map.of("status", "invalid"), session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("状态值无效"));
    }

    @Test
    void toggleStatusSuccess() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        LearningTask task = new LearningTask();
        when(taskService.getById("task-1")).thenReturn(task);

        Result<String> result = controller.toggleStatus("CS101", "task-1", Map.of("status", "closed"), session);

        assertEquals(200, result.getCode());
        assertEquals("closed", task.getStatus());
        verify(taskService).updateById(task);
    }

    // ============ delete ============

    @Test
    void deleteRequiresPermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<Map<String, Object>> result = controller.delete("CS101", "task-1", false, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void deleteNoSubmissions() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(taskService.hasSubmissions("task-1")).thenReturn(false);

        Result<Map<String, Object>> result = controller.delete("CS101", "task-1", false, session);

        assertEquals(200, result.getCode());
        verify(assignmentService).cancelByTaskNo("task-1");
        verify(taskService).removeById("task-1");
    }

    @Test
    void deleteHasSubmissionsNoConfirm() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(taskService.hasSubmissions("task-1")).thenReturn(true);

        Result<Map<String, Object>> result = controller.delete("CS101", "task-1", false, session);

        assertEquals(200, result.getCode());
        assertEquals(true, result.getData().get("hasSubmissions"));
        verify(taskService, never()).removeById(anyString());
    }

    @Test
    void deleteHasSubmissionsWithConfirm() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(taskService.hasSubmissions("task-1")).thenReturn(true);

        Result<Map<String, Object>> result = controller.delete("CS101", "task-1", true, session);

        assertEquals(200, result.getCode());
        verify(assignmentService).cancelByTaskNo("task-1");
        verify(taskService).removeById("task-1");
    }

    // ============ deleteByContract ============

    @Test
    void deleteByContractTaskNotFound() {
        when(taskService.getById("task-1")).thenReturn(null);

        Result<Map<String, Object>> result = controller.deleteByContract("task-1", false, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void deleteByContractSuccess() {
        LearningTask task = new LearningTask();
        task.setTaskNo("task-1");
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(taskService.hasSubmissions("task-1")).thenReturn(false);

        Result<Map<String, Object>> result = controller.deleteByContract("task-1", false, session);

        assertEquals(200, result.getCode());
        verify(taskService).removeById("task-1");
    }

    // ============ taskStats ============

    @Test
    void taskStatsRequiresPermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<Map<String, Object>> result = controller.taskStats("CS101", "task-1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void taskStatsReturnsData() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        TaskSubmission graded = new TaskSubmission();
        graded.setStudentNo("2024001");
        graded.setStatus("graded");
        graded.setScore(85);
        TaskSubmission submitted = new TaskSubmission();
        submitted.setStudentNo("2024002");
        submitted.setStatus("submitted");
        when(submissionService.listByTaskNo("task-1")).thenReturn(List.of(graded, submitted));
        when(assignmentService.countActiveByTaskNo("task-1")).thenReturn(5L);

        Result<Map<String, Object>> result = controller.taskStats("CS101", "task-1", session);

        assertEquals(200, result.getCode());
        assertEquals("task-1", result.getData().get("taskNo"));
        assertEquals(5L, result.getData().get("totalStudents"));
        assertEquals(1, result.getData().get("gradedCount"));
    }

    // ============ taskStatsByContract ============

    @Test
    void taskStatsByContractTaskNotFound() {
        when(taskService.getById("task-1")).thenReturn(null);

        Result<Map<String, Object>> result = controller.taskStatsByContract("task-1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void taskStatsByContractSuccess() {
        LearningTask task = new LearningTask();
        task.setTaskNo("task-1");
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(submissionService.listByTaskNo("task-1")).thenReturn(List.of());
        when(assignmentService.countActiveByTaskNo("task-1")).thenReturn(0L);

        Result<Map<String, Object>> result = controller.taskStatsByContract("task-1", session);

        assertEquals(200, result.getCode());
    }

    // ============ assign ============

    @Test
    void assignTaskNotFound() {
        when(taskService.getById("task-1")).thenReturn(null);

        Result<TaskAssignment> result = controller.assign("task-1", Map.of("studentNo", "2024001"), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void assignRequiresPermission() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<TaskAssignment> result = controller.assign("task-1", Map.of("studentNo", "2024001"), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void assignStudentNoNull() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<TaskAssignment> result = controller.assign("task-1", Map.of(), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void assignStudentNotFound() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(studentService.getById("2024001")).thenReturn(null);

        Result<TaskAssignment> result = controller.assign("task-1", Map.of("studentNo", "2024001"), session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("目标学生不存在"));
    }

    @Test
    void assignSuccess() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        Student student = new Student();
        student.setStudentNo("2024001");
        when(studentService.getById("2024001")).thenReturn(student);
        when(auth.getTeacherId(session)).thenReturn("T001");
        TaskAssignment assignment = new TaskAssignment();
        assignment.setAssignmentId("A1");
        when(assignmentService.assignTask(task, "2024001", "T001", null)).thenReturn(assignment);

        Result<TaskAssignment> result = controller.assign("task-1", Map.of("studentNo", "2024001"), session);

        assertEquals(200, result.getCode());
        assertEquals("A1", result.getData().getAssignmentId());
    }

    @Test
    void assignWithStudentIdParam() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        Student student = new Student();
        student.setStudentNo("2024001");
        when(studentService.getById("2024001")).thenReturn(student);
        when(auth.getTeacherId(session)).thenReturn("T001");
        TaskAssignment assignment = new TaskAssignment();
        when(assignmentService.assignTask(task, "2024001", "T001", null)).thenReturn(assignment);

        Result<TaskAssignment> result = controller.assign("task-1", Map.of("student_id", "2024001"), session);

        assertEquals(200, result.getCode());
    }

    // ============ assignBatch ============

    @Test
    void assignBatchTaskNoNull() {
        Result<List<TaskAssignment>> result = controller.assignBatch(Map.of(), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void assignBatchTaskNotFound() {
        when(taskService.getById("task-1")).thenReturn(null);

        Result<List<TaskAssignment>> result = controller.assignBatch(Map.of("taskNo", "task-1"), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void assignBatchRequiresPermission() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<List<TaskAssignment>> result = controller.assignBatch(Map.of("taskNo", "task-1", "studentNos", List.of("2024001")), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void assignBatchEmptyStudents() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<List<TaskAssignment>> result = controller.assignBatch(Map.of("taskNo", "task-1"), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void assignBatchStudentNotFound() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(studentService.getById("2024001")).thenReturn(null);

        Result<List<TaskAssignment>> result = controller.assignBatch(Map.of("taskNo", "task-1", "studentNos", List.of("2024001")), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void assignBatchSuccess() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        Student s1 = new Student();
        s1.setStudentNo("2024001");
        when(studentService.getById("2024001")).thenReturn(s1);
        when(auth.getTeacherId(session)).thenReturn("T001");
        TaskAssignment a1 = new TaskAssignment();
        a1.setAssignmentId("A1");
        when(assignmentService.assignTask(task, "2024001", "T001", null)).thenReturn(a1);

        Result<List<TaskAssignment>> result = controller.assignBatch(Map.of("taskNo", "task-1", "studentNos", List.of("2024001")), session);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    void assignBatchWithSingleStudentId() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        Student s1 = new Student();
        s1.setStudentNo("2024001");
        when(studentService.getById("2024001")).thenReturn(s1);
        when(auth.getTeacherId(session)).thenReturn("T001");
        TaskAssignment a1 = new TaskAssignment();
        when(assignmentService.assignTask(task, "2024001", "T001", null)).thenReturn(a1);

        Result<List<TaskAssignment>> result = controller.assignBatch(Map.of("taskNo", "task-1", "studentNo", "2024001"), session);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
    }

    // ============ cancelAssignment ============

    @Test
    void cancelAssignmentNotFound() {
        when(assignmentService.getById("A1")).thenReturn(null);

        Result<String> result = controller.cancelAssignment("A1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void cancelAssignmentRequiresPermission() {
        TaskAssignment assignment = new TaskAssignment();
        assignment.setAssignmentId("A1");
        assignment.setCourseCode("CS101");
        when(assignmentService.getById("A1")).thenReturn(assignment);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<String> result = controller.cancelAssignment("A1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void cancelAssignmentSuccess() {
        TaskAssignment assignment = new TaskAssignment();
        assignment.setAssignmentId("A1");
        assignment.setCourseCode("CS101");
        when(assignmentService.getById("A1")).thenReturn(assignment);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<String> result = controller.cancelAssignment("A1", session);

        assertEquals(200, result.getCode());
        assertEquals("cancelled", assignment.getStatus());
        verify(assignmentService).updateById(assignment);
    }
}

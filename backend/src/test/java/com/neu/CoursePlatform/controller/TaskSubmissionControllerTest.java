package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.TaskSubmissionDTO;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.TaskAssignment;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.service.FileStorageService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.TaskAssignmentService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskSubmissionControllerTest {

    @Mock private TaskSubmissionService submissionService;
    @Mock private LearningTaskService taskService;
    @Mock private TaskAssignmentService assignmentService;
    @Mock private FileStorageService fileStorageService;
    @Mock private Auth auth;
    @Mock private HttpSession session;
    @InjectMocks private TaskSubmissionController controller;

    private Student studentSession(String studentNo) {
        Student s = new Student();
        s.setStudentNo(studentNo);
        when(session.getAttribute("student")).thenReturn(s);
        return s;
    }

    private LearningTask buildTask(String taskNo, String courseCode) {
        LearningTask t = new LearningTask();
        t.setTaskNo(taskNo);
        t.setCourseCode(courseCode);
        t.setStatus("published");
        t.setMaxAttempts(3);
        return t;
    }

    // ============ submit ============

    @Test
    void submitTaskNoNull() {
        Result<String> result = controller.submit(null, null, "content", null, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("缺少任务编号"));
    }

    @Test
    void submitTaskNoFromParam() {
        Student s = studentSession("2024001");
        LearningTask task = buildTask("task-1", "CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(assignmentService.getActiveAssignment("task-1", "2024001")).thenReturn(new TaskAssignment());
        when(submissionService.countByStudentAndTask("task-1", "2024001")).thenReturn(0);

        Result<String> result = controller.submit(null, "task-1", "content", null, session);

        assertEquals(200, result.getCode());
    }

    @Test
    void submitNotLoggedIn() {
        when(session.getAttribute("student")).thenReturn(null);

        Result<String> result = controller.submit("task-1", null, "content", null, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("登录"));
    }

    @Test
    void submitTaskNotFound() {
        studentSession("2024001");
        when(taskService.getById("task-1")).thenReturn(null);

        Result<String> result = controller.submit("task-1", null, "content", null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void submitNotAssigned() {
        Student s = studentSession("2024001");
        LearningTask task = buildTask("task-1", "CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(assignmentService.getActiveAssignment("task-1", "2024001")).thenReturn(null);

        Result<String> result = controller.submit("task-1", null, "content", null, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("未分配"));
    }

    @Test
    void submitTaskDraft() {
        Student s = studentSession("2024001");
        LearningTask task = buildTask("task-1", "CS101");
        task.setStatus("draft");
        when(taskService.getById("task-1")).thenReturn(task);
        when(assignmentService.getActiveAssignment("task-1", "2024001")).thenReturn(new TaskAssignment());

        Result<String> result = controller.submit("task-1", null, "content", null, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("尚未发布"));
    }

    @Test
    void submitTaskClosed() {
        Student s = studentSession("2024001");
        LearningTask task = buildTask("task-1", "CS101");
        task.setStatus("closed");
        when(taskService.getById("task-1")).thenReturn(task);
        when(assignmentService.getActiveAssignment("task-1", "2024001")).thenReturn(new TaskAssignment());

        Result<String> result = controller.submit("task-1", null, "content", null, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("已关闭"));
    }

    @Test
    void submitOverdueNoLate() {
        Student s = studentSession("2024001");
        LearningTask task = buildTask("task-1", "CS101");
        task.setDeadline(LocalDateTime.now().minusDays(1));
        task.setAllowLate(0);
        when(taskService.getById("task-1")).thenReturn(task);
        when(assignmentService.getActiveAssignment("task-1", "2024001")).thenReturn(new TaskAssignment());

        Result<String> result = controller.submit("task-1", null, "content", null, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("逾期"));
    }

    @Test
    void submitOverdueWithLateAllowed() {
        Student s = studentSession("2024001");
        LearningTask task = buildTask("task-1", "CS101");
        task.setDeadline(LocalDateTime.now().minusDays(1));
        task.setAllowLate(1);
        when(taskService.getById("task-1")).thenReturn(task);
        when(assignmentService.getActiveAssignment("task-1", "2024001")).thenReturn(new TaskAssignment());
        when(submissionService.countByStudentAndTask("task-1", "2024001")).thenReturn(0);

        Result<String> result = controller.submit("task-1", null, "content", null, session);

        assertEquals(200, result.getCode());
    }

    @Test
    void submitMaxAttemptsExceeded() {
        Student s = studentSession("2024001");
        LearningTask task = buildTask("task-1", "CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(assignmentService.getActiveAssignment("task-1", "2024001")).thenReturn(new TaskAssignment());
        when(submissionService.countByStudentAndTask("task-1", "2024001")).thenReturn(3);

        Result<String> result = controller.submit("task-1", null, "content", null, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("最大提交次数"));
    }

    @Test
    void submitInvalidAttachmentFormat() {
        Student s = studentSession("2024001");
        LearningTask task = buildTask("task-1", "CS101");
        task.setAttachmentFormats(".pdf,.docx");
        when(taskService.getById("task-1")).thenReturn(task);
        when(assignmentService.getActiveAssignment("task-1", "2024001")).thenReturn(new TaskAssignment());
        when(submissionService.countByStudentAndTask("task-1", "2024001")).thenReturn(0);
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", new byte[]{1});

        Result<String> result = controller.submit("task-1", null, null, file, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不允许的文件格式"));
    }

    @Test
    void submitVideoTypeRejected() {
        Student s = studentSession("2024001");
        LearningTask task = buildTask("task-1", "CS101");
        task.setTaskType("video");
        when(taskService.getById("task-1")).thenReturn(task);
        when(assignmentService.getActiveAssignment("task-1", "2024001")).thenReturn(new TaskAssignment());
        when(submissionService.countByStudentAndTask("task-1", "2024001")).thenReturn(0);

        Result<String> result = controller.submit("task-1", null, "content", null, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("无需手动提交"));
    }

    @Test
    void submitReadingTypeRejected() {
        Student s = studentSession("2024001");
        LearningTask task = buildTask("task-1", "CS101");
        task.setTaskType("reading");
        when(taskService.getById("task-1")).thenReturn(task);
        when(assignmentService.getActiveAssignment("task-1", "2024001")).thenReturn(new TaskAssignment());
        when(submissionService.countByStudentAndTask("task-1", "2024001")).thenReturn(0);

        Result<String> result = controller.submit("task-1", null, "content", null, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("无需手动提交"));
    }

    @Test
    void submitContentAndFileBothNull() {
        Student s = studentSession("2024001");
        LearningTask task = buildTask("task-1", "CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(assignmentService.getActiveAssignment("task-1", "2024001")).thenReturn(new TaskAssignment());
        when(submissionService.countByStudentAndTask("task-1", "2024001")).thenReturn(0);

        Result<String> result = controller.submit("task-1", null, null, null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void submitSuccessTextOnly() {
        Student s = studentSession("2024001");
        LearningTask task = buildTask("task-1", "CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(assignmentService.getActiveAssignment("task-1", "2024001")).thenReturn(new TaskAssignment());
        when(submissionService.countByStudentAndTask("task-1", "2024001")).thenReturn(0);

        Result<String> result = controller.submit("task-1", null, "my content", null, session);

        assertEquals(200, result.getCode());
        verify(submissionService).submitWithGrading(any(TaskSubmission.class));
        verify(assignmentService).markSubmitted("task-1", "2024001");
    }

    @Test
    void submitWithFile() throws IOException {
        Student s = studentSession("2024001");
        LearningTask task = buildTask("task-1", "CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(assignmentService.getActiveAssignment("task-1", "2024001")).thenReturn(new TaskAssignment());
        when(submissionService.countByStudentAndTask("task-1", "2024001")).thenReturn(0);
        when(fileStorageService.store(any(), anyString())).thenReturn("file.pdf");
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});

        Result<String> result = controller.submit("task-1", null, "content", file, session);

        assertEquals(200, result.getCode());
        verify(submissionService).submitWithGrading(any(TaskSubmission.class));
    }

    @Test
    void submitFileUploadFails() throws IOException {
        Student s = studentSession("2024001");
        LearningTask task = buildTask("task-1", "CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(assignmentService.getActiveAssignment("task-1", "2024001")).thenReturn(new TaskAssignment());
        when(submissionService.countByStudentAndTask("task-1", "2024001")).thenReturn(0);
        when(fileStorageService.store(any(), anyString())).thenThrow(new IOException("error"));
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});

        Result<String> result = controller.submit("task-1", null, "content", file, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("上传失败"));
    }

    // ============ listByTask ============

    @Test
    void listByTaskTaskNotFound() {
        when(taskService.getById("task-1")).thenReturn(null);

        Result<List<TaskSubmissionDTO>> result = controller.listByTask("task-1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void listByTaskRequiresPermission() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<List<TaskSubmissionDTO>> result = controller.listByTask("task-1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void listByTaskSuccess() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(submissionService.listDtoByTaskNo("task-1")).thenReturn(List.of(new TaskSubmissionDTO()));

        Result<List<TaskSubmissionDTO>> result = controller.listByTask("task-1", session);

        assertEquals(200, result.getCode());
    }

    // ============ listByStudent ============

    @Test
    void listByStudentUnauthorized() {
        when(auth.isAdmin(session)).thenReturn(false);
        when(auth.isTeacher(session)).thenReturn(false);
        when(session.getAttribute("student")).thenReturn(null);

        Result<List<TaskSubmission>> result = controller.listByStudent("2024001", null, null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void listByStudentAdminCanAccess() {
        when(auth.isAdmin(session)).thenReturn(true);
        when(submissionService.listByStudentNo("2024001")).thenReturn(List.of(new TaskSubmission()));

        Result<List<TaskSubmission>> result = controller.listByStudent("2024001", null, null, session);

        assertEquals(200, result.getCode());
    }

    @Test
    void listByStudentTeacherCanAccess() {
        when(auth.isAdmin(session)).thenReturn(false);
        when(auth.isTeacher(session)).thenReturn(true);
        when(submissionService.listByStudentNo("2024001")).thenReturn(List.of());

        Result<List<TaskSubmission>> result = controller.listByStudent("2024001", null, null, session);

        assertEquals(200, result.getCode());
    }

    @Test
    void listByStudentOwnStudentCanAccess() {
        when(auth.isAdmin(session)).thenReturn(false);
        when(auth.isTeacher(session)).thenReturn(false);
        Student s = new Student();
        s.setStudentNo("2024001");
        when(session.getAttribute("student")).thenReturn(s);
        when(submissionService.listByStudentNo("2024001")).thenReturn(List.of());

        Result<List<TaskSubmission>> result = controller.listByStudent("2024001", null, null, session);

        assertEquals(200, result.getCode());
    }

    @Test
    void listByStudentFiltersByCourseCode() {
        when(auth.isAdmin(session)).thenReturn(true);
        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");
        when(submissionService.listByStudentNo("2024001")).thenReturn(List.of(sub));
        when(submissionService.getTaskCourseCode("task-1")).thenReturn("CS101");

        Result<List<TaskSubmission>> result = controller.listByStudent("2024001", "CS101", null, session);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    void listByStudentFiltersByCourseIdParam() {
        when(auth.isAdmin(session)).thenReturn(true);
        when(submissionService.listByStudentNo("2024001")).thenReturn(List.of());

        Result<List<TaskSubmission>> result = controller.listByStudent("2024001", null, "CS101", session);

        assertEquals(200, result.getCode());
    }

    // ============ listMy ============

    @Test
    void listMyNotLoggedIn() {
        when(session.getAttribute("student")).thenReturn(null);

        Result<List<TaskSubmission>> result = controller.listMy(session);

        assertEquals(500, result.getCode());
    }

    @Test
    void listMySuccess() {
        Student s = new Student();
        s.setStudentNo("2024001");
        when(session.getAttribute("student")).thenReturn(s);
        when(submissionService.listByStudentNo("2024001")).thenReturn(List.of(new TaskSubmission()));

        Result<List<TaskSubmission>> result = controller.listMy(session);

        assertEquals(200, result.getCode());
    }

    // ============ gradeDetail ============

    @Test
    void gradeDetailSubmissionNotFound() {
        when(submissionService.getById("sub-1")).thenReturn(null);

        Result<Map<String, Object>> result = controller.gradeDetail("sub-1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void gradeDetailRequiresPermission() {
        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<Map<String, Object>> result = controller.gradeDetail("sub-1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void gradeDetailSuccess() {
        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(submissionService.buildGradeDetail("sub-1")).thenReturn(Map.of("score", 85));

        Result<Map<String, Object>> result = controller.gradeDetail("sub-1", session);

        assertEquals(200, result.getCode());
        assertEquals(85, result.getData().get("score"));
    }

    // ============ grade ============

    @Test
    void gradeSubmissionNotFound() {
        when(submissionService.getById("sub-1")).thenReturn(null);

        Result<Void> result = controller.grade("sub-1", new TaskSubmission(), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void gradeRequiresPermission() {
        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<Void> result = controller.grade("sub-1", new TaskSubmission(), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void gradeWithScore() {
        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");
        sub.setStudentNo("2024001");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        TaskSubmission body = new TaskSubmission();
        body.setScore(90);
        body.setFeedback("Good job");

        Result<Void> result = controller.grade("sub-1", body, session);

        assertEquals(200, result.getCode());
        assertEquals(90, sub.getScore());
        assertEquals("Good job", sub.getFeedback());
        assertEquals("graded", sub.getStatus());
        verify(submissionService).updateById(sub);
    }

    @Test
    void gradeWithoutScoreAutoScores() {
        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo("task-1");
        sub.setStudentNo("2024001");
        when(submissionService.getById("sub-1")).thenReturn(sub);
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(submissionService.autoScoreChoices(sub)).thenReturn(75);

        Result<Void> result = controller.grade("sub-1", new TaskSubmission(), session);

        assertEquals(200, result.getCode());
        assertEquals(75, sub.getScore());
        assertEquals("graded", sub.getStatus());
        verify(assignmentService).markCompleted("task-1", "2024001");
    }
}

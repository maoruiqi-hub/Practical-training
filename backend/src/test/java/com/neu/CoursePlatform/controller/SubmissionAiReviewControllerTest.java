package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.SubmissionAiReview;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.SubmissionAiReviewService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionAiReviewControllerTest {

    @Mock
    private SubmissionAiReviewService aiReviewService;

    @Mock
    private TaskSubmissionService submissionService;

    @Mock
    private LearningTaskService taskService;

    @Mock
    private Auth auth;

    @Mock
    private HttpSession session;

    @InjectMocks
    private SubmissionAiReviewController controller;

    // ======================== generate (POST) ========================

    @Test
    void generate_NotLoggedIn() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<SubmissionAiReview> result = controller.generate("sub001", session);

        assertEquals(500, result.getCode());
        assertEquals("请先登录", result.getMsg());
        verify(aiReviewService, never()).generateReview(anyString());
    }

    @Test
    void generate_SubmissionNotFound() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(submissionService.getById("sub001")).thenReturn(null);

        Result<SubmissionAiReview> result = controller.generate("sub001", session);

        assertEquals(500, result.getCode());
        assertEquals("提交记录不存在", result.getMsg());
        verify(aiReviewService, never()).generateReview(anyString());
    }

    @Test
    void generate_StudentOwnsSubmission_Success() {
        Student student = new Student();
        student.setStudentNo("S1001");
        TaskSubmission submission = submissionWith("S1001", "task01");

        when(auth.isLoggedIn(session)).thenReturn(true);
        when(submissionService.getById("sub001")).thenReturn(submission);
        when(auth.getStudent(session)).thenReturn(student);

        SubmissionAiReview review = new SubmissionAiReview();
        review.setReviewId("r001");
        when(aiReviewService.generateReview("sub001")).thenReturn(review);

        Result<SubmissionAiReview> result = controller.generate("sub001", session);

        assertEquals(200, result.getCode());
        assertEquals("r001", result.getData().getReviewId());
    }

    @Test
    void generate_StudentCannotAccessOthersSubmission() {
        Student student = new Student();
        student.setStudentNo("S1001");
        TaskSubmission submission = submissionWith("S2002", "task01");

        when(auth.isLoggedIn(session)).thenReturn(true);
        when(submissionService.getById("sub001")).thenReturn(submission);
        when(auth.getStudent(session)).thenReturn(student);

        Result<SubmissionAiReview> result = controller.generate("sub001", session);

        assertEquals(500, result.getCode());
        assertEquals("无权查看他人的评阅", result.getMsg());
        verify(aiReviewService, never()).generateReview(anyString());
    }

    @Test
    void generate_TeacherWithCoursePermission_Success() {
        Teacher teacher = teacherWith("T001");
        TaskSubmission submission = submissionWith("S1001", "task01");
        LearningTask task = taskWith("C001");

        when(auth.isLoggedIn(session)).thenReturn(true);
        when(submissionService.getById("sub001")).thenReturn(submission);
        when(auth.getStudent(session)).thenReturn(null);
        when(auth.getTeacher(session)).thenReturn(teacher);
        when(taskService.getById("task01")).thenReturn(task);
        when(auth.canModifyCourse(session, "C001")).thenReturn(true);

        SubmissionAiReview review = new SubmissionAiReview();
        review.setReviewId("r002");
        when(aiReviewService.generateReview("sub001")).thenReturn(review);

        Result<SubmissionAiReview> result = controller.generate("sub001", session);

        assertEquals(200, result.getCode());
        assertEquals("r002", result.getData().getReviewId());
    }

    @Test
    void generate_TeacherCannotModifyCourse() {
        Teacher teacher = teacherWith("T001");
        TaskSubmission submission = submissionWith("S1001", "task01");
        LearningTask task = taskWith("C002");

        when(auth.isLoggedIn(session)).thenReturn(true);
        when(submissionService.getById("sub001")).thenReturn(submission);
        when(auth.getStudent(session)).thenReturn(null);
        when(auth.getTeacher(session)).thenReturn(teacher);
        when(taskService.getById("task01")).thenReturn(task);
        when(auth.canModifyCourse(session, "C002")).thenReturn(false);

        Result<SubmissionAiReview> result = controller.generate("sub001", session);

        assertEquals(500, result.getCode());
        assertEquals("无权评阅该课程的任务", result.getMsg());
        verify(aiReviewService, never()).generateReview(anyString());
    }

    @Test
    void generate_Teacher_TaskNotFound() {
        Teacher teacher = teacherWith("T001");
        TaskSubmission submission = submissionWith("S1001", "task01");

        when(auth.isLoggedIn(session)).thenReturn(true);
        when(submissionService.getById("sub001")).thenReturn(submission);
        when(auth.getStudent(session)).thenReturn(null);
        when(auth.getTeacher(session)).thenReturn(teacher);
        when(taskService.getById("task01")).thenReturn(null);

        Result<SubmissionAiReview> result = controller.generate("sub001", session);

        assertEquals(500, result.getCode());
        assertEquals("无权评阅该课程的任务", result.getMsg());
        verify(aiReviewService, never()).generateReview(anyString());
    }

    @Test
    void generate_IllegalArgumentException_ReturnsFailWithMessage() {
        Student student = new Student();
        student.setStudentNo("S1001");
        TaskSubmission submission = submissionWith("S1001", "task01");

        when(auth.isLoggedIn(session)).thenReturn(true);
        when(submissionService.getById("sub001")).thenReturn(submission);
        when(auth.getStudent(session)).thenReturn(student);
        when(aiReviewService.generateReview("sub001"))
                .thenThrow(new IllegalArgumentException("在线测验请使用系统评阅和教师复核"));

        Result<SubmissionAiReview> result = controller.generate("sub001", session);

        assertEquals(500, result.getCode());
        assertEquals("在线测验请使用系统评阅和教师复核", result.getMsg());
    }

    @Test
    void generate_GenericException_ReturnsFailWithFallbackMessage() {
        Student student = new Student();
        student.setStudentNo("S1001");
        TaskSubmission submission = submissionWith("S1001", "task01");

        when(auth.isLoggedIn(session)).thenReturn(true);
        when(submissionService.getById("sub001")).thenReturn(submission);
        when(auth.getStudent(session)).thenReturn(student);
        when(aiReviewService.generateReview("sub001"))
                .thenThrow(new RuntimeException("Connection timeout"));

        Result<SubmissionAiReview> result = controller.generate("sub001", session);

        assertEquals(500, result.getCode());
        assertEquals("AI 评阅生成失败，请稍后重试", result.getMsg());
    }

    // ======================== get (GET) ========================

    @Test
    void get_NotLoggedIn() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<SubmissionAiReview> result = controller.get("sub001", session);

        assertEquals(500, result.getCode());
        assertEquals("请先登录", result.getMsg());
        verify(aiReviewService, never()).getLatestBySubmissionId(anyString());
    }

    @Test
    void get_SubmissionNotFound() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(submissionService.getById("sub001")).thenReturn(null);

        Result<SubmissionAiReview> result = controller.get("sub001", session);

        assertEquals(500, result.getCode());
        assertEquals("提交记录不存在", result.getMsg());
        verify(aiReviewService, never()).getLatestBySubmissionId(anyString());
    }

    @Test
    void get_StudentOwnsSubmission_Success() {
        Student student = new Student();
        student.setStudentNo("S1001");
        TaskSubmission submission = submissionWith("S1001", "task01");

        when(auth.isLoggedIn(session)).thenReturn(true);
        when(submissionService.getById("sub001")).thenReturn(submission);
        when(auth.getStudent(session)).thenReturn(student);

        SubmissionAiReview review = new SubmissionAiReview();
        review.setReviewId("r001");
        when(aiReviewService.getLatestBySubmissionId("sub001")).thenReturn(review);

        Result<SubmissionAiReview> result = controller.get("sub001", session);

        assertEquals(200, result.getCode());
        assertEquals("r001", result.getData().getReviewId());
    }

    @Test
    void get_StudentOwnsSubmission_NoReviewYet() {
        Student student = new Student();
        student.setStudentNo("S1001");
        TaskSubmission submission = submissionWith("S1001", "task01");

        when(auth.isLoggedIn(session)).thenReturn(true);
        when(submissionService.getById("sub001")).thenReturn(submission);
        when(auth.getStudent(session)).thenReturn(student);
        when(aiReviewService.getLatestBySubmissionId("sub001")).thenReturn(null);

        Result<SubmissionAiReview> result = controller.get("sub001", session);

        assertEquals(200, result.getCode());
        assertNull(result.getData());
    }

    @Test
    void get_StudentCannotAccessOthersReview() {
        Student student = new Student();
        student.setStudentNo("S1001");
        TaskSubmission submission = submissionWith("S2002", "task01");

        when(auth.isLoggedIn(session)).thenReturn(true);
        when(submissionService.getById("sub001")).thenReturn(submission);
        when(auth.getStudent(session)).thenReturn(student);

        Result<SubmissionAiReview> result = controller.get("sub001", session);

        assertEquals(500, result.getCode());
        assertEquals("无权查看他人的评阅", result.getMsg());
        verify(aiReviewService, never()).getLatestBySubmissionId(anyString());
    }

    @Test
    void get_TeacherWithCoursePermission_Success() {
        Teacher teacher = teacherWith("T001");
        TaskSubmission submission = submissionWith("S1001", "task01");
        LearningTask task = taskWith("C001");

        when(auth.isLoggedIn(session)).thenReturn(true);
        when(submissionService.getById("sub001")).thenReturn(submission);
        when(auth.getStudent(session)).thenReturn(null);
        when(auth.getTeacher(session)).thenReturn(teacher);
        when(taskService.getById("task01")).thenReturn(task);
        when(auth.canModifyCourse(session, "C001")).thenReturn(true);

        SubmissionAiReview review = new SubmissionAiReview();
        review.setReviewId("r002");
        when(aiReviewService.getLatestBySubmissionId("sub001")).thenReturn(review);

        Result<SubmissionAiReview> result = controller.get("sub001", session);

        assertEquals(200, result.getCode());
        assertEquals("r002", result.getData().getReviewId());
    }

    @Test
    void get_TeacherCannotModifyCourse() {
        Teacher teacher = teacherWith("T001");
        TaskSubmission submission = submissionWith("S1001", "task01");
        LearningTask task = taskWith("C002");

        when(auth.isLoggedIn(session)).thenReturn(true);
        when(submissionService.getById("sub001")).thenReturn(submission);
        when(auth.getStudent(session)).thenReturn(null);
        when(auth.getTeacher(session)).thenReturn(teacher);
        when(taskService.getById("task01")).thenReturn(task);
        when(auth.canModifyCourse(session, "C002")).thenReturn(false);

        Result<SubmissionAiReview> result = controller.get("sub001", session);

        assertEquals(500, result.getCode());
        assertEquals("无权评阅该课程的任务", result.getMsg());
        verify(aiReviewService, never()).getLatestBySubmissionId(anyString());
    }

    // ======================== helpers ========================

    private TaskSubmission submissionWith(String studentNo, String taskNo) {
        TaskSubmission submission = new TaskSubmission();
        submission.setSubmissionId("sub001");
        submission.setStudentNo(studentNo);
        submission.setTaskNo(taskNo);
        return submission;
    }

    private LearningTask taskWith(String courseCode) {
        LearningTask task = new LearningTask();
        task.setTaskNo("task01");
        task.setCourseCode(courseCode);
        return task;
    }

    private Teacher teacherWith(String teacherNo) {
        Teacher teacher = new Teacher();
        teacher.setTeacherNo(teacherNo);
        teacher.setRole("teacher");
        return teacher;
    }
}

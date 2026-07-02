package com.neu.CoursePlatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.ExamGenerateRequest;
import com.neu.CoursePlatform.dto.ExamGenerateResult;
import com.neu.CoursePlatform.entity.Exam;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.service.ExamService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.QuestionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamControllerTest {

    @Mock private QuestionService questionService;
    @Mock private ExamService examService;
    @Mock private LearningTaskService taskService;
    @Mock private Auth auth;
    @Mock private HttpSession session;
    @InjectMocks private ExamController controller;

    // ============ list ============

    @Test
    void listRequiresLoginWhenNoCourseCode() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<List<Exam>> result = controller.list(null, null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void listRequiresCoursePermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<List<Exam>> result = controller.list("CS101", null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void listReturnsExamsForCourse() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(examService.list(any(QueryWrapper.class))).thenReturn(List.of(new Exam()));

        Result<List<Exam>> result = controller.list("CS101", null, session);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    void listUsesCourseIdAsFallback() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(examService.list(any(QueryWrapper.class))).thenReturn(List.of());

        Result<List<Exam>> result = controller.list(null, "CS101", session);

        assertEquals(200, result.getCode());
    }

    // ============ detail ============

    @Test
    void detailRequiresLogin() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<Exam> result = controller.detail("exam-1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void detailReturnsExam() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        Exam exam = new Exam();
        exam.setExamId("exam-1");
        exam.setCourseCode("CS101");
        when(examService.getById("exam-1")).thenReturn(exam);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<Exam> result = controller.detail("exam-1", session);

        assertEquals(200, result.getCode());
    }

    @Test
    void detailNotFound() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(examService.getById("nonexistent")).thenReturn(null);

        Result<Exam> result = controller.detail("nonexistent", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void detailPermissionDenied() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        Exam exam = new Exam();
        exam.setExamId("exam-1");
        exam.setCourseCode("CS101");
        when(examService.getById("exam-1")).thenReturn(exam);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<Exam> result = controller.detail("exam-1", session);

        assertEquals(500, result.getCode());
    }

    // ============ generate ============

    @Test
    void generateRequiresPermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<List<Question>> result = controller.generateExam("CS101", new ExamGenerateRequest(), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void generateReturnsQuestions() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        ExamGenerateRequest req = new ExamGenerateRequest();
        when(questionService.generateExam("CS101", req)).thenReturn(List.of(new Question()));

        Result<List<Question>> result = controller.generateExam("CS101", req, session);

        assertEquals(200, result.getCode());
    }

    @Test
    void generateHandlesIllegalArgument() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        ExamGenerateRequest req = new ExamGenerateRequest();
        when(questionService.generateExam("CS101", req)).thenThrow(new IllegalArgumentException("题目数量不足"));

        Result<List<Question>> result = controller.generateExam("CS101", req, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("题目数量不足"));
    }

    // ============ generateAndSave ============

    @Test
    void generateAndSaveRequiresPermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<ExamGenerateResult> result = controller.generateAndSaveExam("CS101", new ExamGenerateRequest(), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void generateAndSaveReturnsResult() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        ExamGenerateRequest req = new ExamGenerateRequest();
        ExamGenerateResult expected = new ExamGenerateResult(new Exam(), List.of());
        when(examService.generateAndSave("CS101", req)).thenReturn(expected);

        Result<ExamGenerateResult> result = controller.generateAndSaveExam("CS101", req, session);

        assertEquals(200, result.getCode());
    }

    @Test
    void generateAndSaveHandlesIllegalArgument() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        ExamGenerateRequest req = new ExamGenerateRequest();
        when(examService.generateAndSave("CS101", req)).thenThrow(new IllegalArgumentException("参数错误"));

        Result<ExamGenerateResult> result = controller.generateAndSaveExam("CS101", req, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("参数错误"));
    }

    // ============ bindExamToTask ============

    @Test
    void bindTaskNotFound() {
        when(taskService.getById("task-x")).thenReturn(null);

        Result<Void> result = controller.bindExamToTask("exam-1", "task-x", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void bindRequiresCoursePermission() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<Void> result = controller.bindExamToTask("exam-1", "task-1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void bindSuccess() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<Void> result = controller.bindExamToTask("exam-1", "task-1", session);

        assertEquals(200, result.getCode());
        verify(examService).bindToTask("exam-1", "task-1");
    }

    @Test
    void bindHandlesIllegalArgument() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        doThrow(new IllegalArgumentException("试卷已绑定")).when(examService).bindToTask("exam-1", "task-1");

        Result<Void> result = controller.bindExamToTask("exam-1", "task-1", session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("试卷已绑定"));
    }
}

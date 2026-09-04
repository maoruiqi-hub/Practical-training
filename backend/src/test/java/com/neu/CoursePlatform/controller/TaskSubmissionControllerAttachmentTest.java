package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.TaskAssignment;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.service.FileStorageService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.TaskAssignmentService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskSubmissionControllerAttachmentTest {

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rollsBackPromotedAttachmentWhenSubmissionTransactionFails() throws Exception {
        TaskSubmissionService submissionService = mock(TaskSubmissionService.class);
        LearningTaskService taskService = mock(LearningTaskService.class);
        TaskAssignmentService assignmentService = mock(TaskAssignmentService.class);
        FileStorageService fileStorageService = mock(FileStorageService.class);
        Auth auth = mock(Auth.class);
        HttpSession session = mock(HttpSession.class);
        Student student = new Student();
        student.setStudentNo("student-1");
        LearningTask task = new LearningTask();
        task.setTaskNo("task-1");
        task.setStatus("published");
        task.setTaskType("homework");
        task.setMaxAttempts(3);
        task.setAttachmentFormats(".txt");

        when(session.getAttribute("student")).thenReturn(student);
        when(taskService.getById("task-1")).thenReturn(task);
        when(assignmentService.getActiveAssignment("task-1", "student-1")).thenReturn(new TaskAssignment());
        when(submissionService.countByStudentAndTask("task-1", "student-1")).thenReturn(0);
        when(fileStorageService.storeTemporary(any(), anyString())).thenReturn("resource/.upload-tmp/HomeworkUpload/temp.txt");
        when(fileStorageService.promoteTemporary(anyString(), anyString())).thenReturn("resource/HomeworkUpload/final.txt");

        TaskSubmissionController controller = new TaskSubmissionController(
                submissionService, taskService, assignmentService, fileStorageService, auth);
        TransactionSynchronizationManager.initSynchronization();

        controller.submit("task-1", null, "content",
                new MockMultipartFile("file", "report.txt", "text/plain", "data".getBytes()), session);

        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        assertTrue(synchronizations.stream().anyMatch(s -> s != null));
        synchronizations.forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(fileStorageService).deleteStoredFileIfExists("resource/HomeworkUpload/final.txt");
        verify(fileStorageService).deleteStoredFileIfExists("resource/.upload-tmp/HomeworkUpload/temp.txt");
    }

    @Test
    void recordsTeacherInterventionAndUsesServerRecalculatedQuizScore() {
        TaskSubmissionService submissionService = mock(TaskSubmissionService.class);
        LearningTaskService taskService = mock(LearningTaskService.class);
        TaskAssignmentService assignmentService = mock(TaskAssignmentService.class);
        FileStorageService fileStorageService = mock(FileStorageService.class);
        Auth auth = mock(Auth.class);
        HttpSession session = mock(HttpSession.class);
        Student student = new Student();
        student.setStudentNo("student-1");
        Teacher teacher = new Teacher();
        teacher.setTeacherNo("teacher-1");
        teacher.setRole("teacher");
        LearningTask task = new LearningTask();
        task.setTaskNo("task-1");
        task.setCourseCode("course-1");
        task.setTaskType("quiz");
        task.setScore(100);
        TaskSubmission existing = new TaskSubmission();
        existing.setSubmissionId("submission-1");
        existing.setTaskNo("task-1");
        existing.setStudentNo("student-1");
        existing.setScore(60);
        existing.setStatus("submitted");

        when(session.getAttribute("teacher")).thenReturn(teacher);
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "course-1")).thenReturn(true);
        when(auth.getTeacher(session)).thenReturn(teacher);
        when(submissionService.getById("submission-1")).thenReturn(existing);
        when(submissionService.recalculateFinalScore(existing)).thenReturn(85);

        TaskSubmissionController controller = new TaskSubmissionController(
                submissionService, taskService, assignmentService, fileStorageService, auth);
        TaskSubmission body = new TaskSubmission();
        body.setFeedback("复核发现题目答案存在边界情况");
        body.setManualAnswers(List.of(Map.of("questionId", "q1", "score", 25, "correct", false)));

        Result<Void> result = controller.grade("submission-1", body, session);

        assertEquals(200, result.getCode());
        assertEquals(85, existing.getScore());
        assertEquals(60, existing.getPreviousScore());
        assertEquals("复核发现题目答案存在边界情况", existing.getInterventionReason());
        assertEquals("teacher-1", existing.getInterventionBy());
        assertNotNull(existing.getInterventionAt());
        verify(submissionService).recordReviewedSubjectiveEvidence(existing, body.getManualAnswers());
        verify(submissionService).recalculateFinalScore(existing);
        verify(submissionService).updateById(existing);
    }
}

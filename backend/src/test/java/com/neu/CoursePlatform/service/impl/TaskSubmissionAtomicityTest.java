package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.mapper.TaskSubmissionMapper;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.TaskQuestionService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskSubmissionAtomicityTest {

    @Test
    void invalidQuizIsRejectedBeforeAnySubmissionIsSuperseded() {
        LearningTask task = task("quiz");
        LearningTaskService taskService = mock(LearningTaskService.class);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(true);
        TaskQuestionService taskQuestionService = mock(TaskQuestionService.class);
        when(taskQuestionService.listByTaskNo("task-1")).thenReturn(List.of());
        TaskSubmissionMapper mapper = mock(TaskSubmissionMapper.class);
        TaskSubmissionServiceImpl service = service(taskService, taskQuestionService, mapper);

        assertThrows(IllegalArgumentException.class,
                () -> service.submitWithGrading(submission("[]")));

        verify(mapper, never()).insert(any(TaskSubmission.class));
        verify(mapper, never()).markSupersededPrevious(any(), any(), any());
    }

    @Test
    void oldSubmissionsAreSupersededOnlyAfterNewSubmissionIsPersisted() {
        LearningTask task = task("homework");
        LearningTaskService taskService = mock(LearningTaskService.class);
        when(taskService.getById("task-1")).thenReturn(task);
        when(taskService.isQuizTask(task)).thenReturn(false);
        TaskSubmissionMapper mapper = mock(TaskSubmissionMapper.class);
        when(mapper.insert(any(TaskSubmission.class))).thenAnswer(invocation -> {
            TaskSubmission inserted = invocation.getArgument(0);
            inserted.setSubmissionId("new-submission");
            return 1;
        });
        TaskSubmissionServiceImpl service = service(taskService, mock(TaskQuestionService.class), mapper);

        service.submitWithGrading(submission("answer"));

        InOrder order = inOrder(mapper);
        order.verify(mapper).insert(any(TaskSubmission.class));
        order.verify(mapper).markSupersededPrevious("task-1", "student-1", "new-submission");
    }

    private TaskSubmissionServiceImpl service(LearningTaskService taskService,
                                              TaskQuestionService taskQuestionService,
                                              TaskSubmissionMapper mapper) {
        TaskSubmissionServiceImpl service = new TaskSubmissionServiceImpl(
                taskService, null, null, null, null, null, null, event -> { }, null, taskQuestionService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        return service;
    }

    private LearningTask task(String type) {
        LearningTask task = new LearningTask();
        task.setTaskNo("task-1");
        task.setTaskType(type);
        return task;
    }

    private TaskSubmission submission(String content) {
        TaskSubmission submission = new TaskSubmission();
        submission.setTaskNo("task-1");
        submission.setStudentNo("student-1");
        submission.setContent(content);
        return submission;
    }
}

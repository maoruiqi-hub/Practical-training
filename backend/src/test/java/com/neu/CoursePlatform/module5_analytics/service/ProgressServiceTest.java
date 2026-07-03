package com.neu.CoursePlatform.module5_analytics.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.module5_analytics.dto.ClassProgressDTO;
import com.neu.CoursePlatform.module5_analytics.dto.TaskCompletionDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentProgressDTO;
import com.neu.CoursePlatform.module5_analytics.service.external.ExternalDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class ProgressServiceTest {

    private ExternalDataProvider dataProvider;
    private ProgressService service;

    @BeforeEach
    void setUp() {
        dataProvider = mock(ExternalDataProvider.class);
        service = new ProgressService(dataProvider);
    }

    @Test
    void getClassProgressComputesAveragesAndLaggingStudents() {
        when(dataProvider.getClassProgressList("class-1", "c1")).thenReturn(List.of(
                progress("s1", "张三", 10, 9, 0.90),
                progress("s2", "李四", 10, 5, 0.50),
                progress("s3", "王五", 10, 2, 0.20)));

        ClassProgressDTO result = service.getClassProgress("class-1", "c1");

        assertEquals(0.53, result.getAvgCompletionRate());
        assertEquals(10, result.getTotalTasks());
        assertEquals(5.33, result.getAvgCompletedTasks());
        assertEquals(1, result.getLaggingStudents().size());
        assertEquals("s3", result.getLaggingStudents().get(0).getStudentId());
        assertEquals(0.33, result.getLaggingStudents().get(0).getGapFromAvg());
    }

    @Test
    void getClassProgressHandlesEmptyClassAndDelegatesTaskCompletion() {
        when(dataProvider.getClassProgressList("empty", "c1")).thenReturn(List.of());
        TaskCompletionDTO taskCompletion = new TaskCompletionDTO();
        taskCompletion.setTaskId("t1");
        when(dataProvider.getTaskCompletion("empty", "t1")).thenReturn(taskCompletion);

        ClassProgressDTO result = service.getClassProgress("empty", "c1");

        assertEquals(0, result.getAvgCompletionRate());
        assertEquals(0, result.getTotalTasks());
        assertTrue(result.getLaggingStudents().isEmpty());
        assertSame(taskCompletion, service.getTaskCompletion("empty", "t1"));
    }

    private static StudentProgressDTO progress(String id, String name, int total, int completed, double rate) {
        StudentProgressDTO dto = new StudentProgressDTO();
        dto.setStudentId(id);
        dto.setStudentName(name);
        dto.setTotalTasks(total);
        dto.setCompletedTasks(completed);
        dto.setCompletionRate(rate);
        return dto;
    }
}

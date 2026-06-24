package com.neu.CoursePlatform.module5_analytics.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 任务完成率详情
 */
@Data
@NoArgsConstructor
public class TaskCompletionDTO {
    private String taskId;
    private String taskName;
    private int totalStudents;
    private int submittedCount;
    private int notSubmittedCount;
    private int lateSubmittedCount;
    private double submissionRate;
    private List<String> notSubmittedStudentIds;
}

package com.neu.CoursePlatform.module5_analytics.dto.external;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生学习进度（来自模块2）
 */
@Data
@NoArgsConstructor
public class StudentProgressDTO {
    private String studentId;
    private String studentName;
    private int totalTasks;
    private int completedTasks;
    private int submittedTasks;
    private double completionRate;  // 0.0 ~ 1.0
}

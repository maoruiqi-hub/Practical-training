package com.neu.CoursePlatform.module5_analytics.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 班级学习进度总览
 */
@Data
@NoArgsConstructor
public class ClassProgressDTO {
    private double avgCompletionRate;         // 平均完成率
    private int totalTasks;
    private double avgCompletedTasks;
    private List<LaggingStudent> laggingStudents;  // 进度落后学生

    @Data
    @NoArgsConstructor
    public static class LaggingStudent {
        private String studentId;
        private String studentName;
        private double completionRate;
        private double gapFromAvg;  // 与班级平均的差距
    }
}

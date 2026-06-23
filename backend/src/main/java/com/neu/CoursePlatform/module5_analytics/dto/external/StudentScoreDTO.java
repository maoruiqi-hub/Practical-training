package com.neu.CoursePlatform.module5_analytics.dto.external;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学生成绩记录（来自模块3）
 */
@Data
@NoArgsConstructor
public class StudentScoreDTO {
    private String studentId;
    private String studentName;
    private String courseId;
    private String targetId;       // 考试/任务ID
    private String targetType;     // exam / task
    private Double score;
    private Double totalScore;
    private LocalDateTime scoredAt;
}

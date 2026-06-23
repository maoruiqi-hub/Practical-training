package com.neu.CoursePlatform.module5_analytics.dto.external;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 班级错题统计（来自模块3）
 */
@Data
@NoArgsConstructor
public class MistakeStatsDTO {
    private String knowledgePointId;
    private String knowledgePointName;
    private int totalAttempts;
    private int mistakeCount;
    private double mistakeRate;     // 0.0 ~ 1.0
}

package com.neu.CoursePlatform.module5_analytics.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 薄弱知识点
 */
@Data
@NoArgsConstructor
public class WeakPointDTO {
    private String knowledgePointId;
    private String knowledgePointName;
    private double scoreRate;        // 得分率 0.0~1.0
    private int totalAttempts;
    private int mistakeCount;
}

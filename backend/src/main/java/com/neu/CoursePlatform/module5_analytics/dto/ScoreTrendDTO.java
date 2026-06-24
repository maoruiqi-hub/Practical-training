package com.neu.CoursePlatform.module5_analytics.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 成绩趋势数据
 */
@Data
@NoArgsConstructor
public class ScoreTrendDTO {
    private List<TrendPoint> classAvg;      // 班级平均分趋势
    private List<TrendPoint> studentScore;   // 学生个人得分趋势（可选）
    private String granularity;              // week / exam

    @Data
    @NoArgsConstructor
    public static class TrendPoint {
        private String label;    // "W1" / "期中考试"
        private double value;
    }
}

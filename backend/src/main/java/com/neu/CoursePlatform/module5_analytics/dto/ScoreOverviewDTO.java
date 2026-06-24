package com.neu.CoursePlatform.module5_analytics.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 班级成绩总览
 */
@Data
@NoArgsConstructor
public class ScoreOverviewDTO {
    private double avgScore;
    private double maxScore;
    private double minScore;
    private double stdDev;
    private double passRate;            // ≥60 比例
    private List<ScoreDistributionDTO> distribution;
    private List<StudentRanking> rankings;

    @Data
    @NoArgsConstructor
    public static class StudentRanking {
        private String studentId;
        private String studentName;
        private double totalScore;
        private int rank;
    }
}

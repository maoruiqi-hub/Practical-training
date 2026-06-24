package com.neu.CoursePlatform.module5_analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分数段分布
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreDistributionDTO {
    private String range;   // "0-59" / "60-69" / "70-79" / "80-89" / "90-100"
    private int count;
    private double percentage;
}

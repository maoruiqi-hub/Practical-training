package com.neu.CoursePlatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MistakeStatsDTO {
    private String knowledgePointId;
    private String questionType;
    private long attemptCount;
    private long wrongCount;
    private double wrongRate;
}

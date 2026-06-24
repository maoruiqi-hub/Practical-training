package com.neu.CoursePlatform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScoreRecordDTO {
    private String studentNo;
    private String taskNo;
    private String submissionId;
    private String courseCode;
    private Integer score;
    private Integer totalScore;
    private String status;
    private LocalDateTime submitTime;
}

package com.neu.CoursePlatform.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务提交 DTO（含学生名、任务类型）
 */
@Data
public class TaskSubmissionDTO {
    private String submissionId;
    private String taskNo;
    private String taskType;
    private String studentNo;
    private String studentName;
    private String content;
    private String filePath;
    private LocalDateTime submitTime;
    private Integer score;
    private String status;
    private String feedback;
}

package com.neu.CoursePlatform.dto;

import lombok.Data;

/**
 * 任务更新请求 DTO（JSON body）
 */
@Data
public class TaskUpdateRequest {
    private String taskName;
    private String lessonNo;
    private String knowledgePoints;
    private String taskType;
    private String description;
    private String deadline;
    private String submitMethod;
    private Integer score;
    private String gradingRule;
    private String status;
    private Integer allowLate;
    private Integer maxAttempts;
    private String attachmentFormats;
}

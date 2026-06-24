package com.neu.CoursePlatform.dto;

import lombok.Data;

/** Safe exam question projection for student/tower clients; it never exposes the answer. */
@Data
public class ExamQuestionViewDTO {
    private String questionId;
    private Integer sortOrder;
    private Integer score;
    private String type;
    private String knowledgePointId;
    private Integer difficulty;
    private String stem;
    private String options;
}

package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("learning_answer_evidence")
public class LearningAnswerEvidence {
    @TableId(value = "evidence_id", type = IdType.ASSIGN_UUID)
    private String evidenceId;
    private String studentNo;
    private String courseCode;
    private String questionId;
    private String knowledgePointId;
    private Integer difficulty;
    private Integer attemptNo;
    private Boolean firstAttempt;
    private Boolean correct;
    private String answerContent;
    private String sourceType;
    private String sourceId;
    private String idempotencyKey;
    private String formulaVersion;
    private LocalDateTime answeredAt;
}

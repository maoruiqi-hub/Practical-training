package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("student_tower_attempt")
public class StudentTowerAttempt {
    @TableId(value = "attempt_id", type = IdType.ASSIGN_UUID)
    private String attemptId;
    private String runId;
    private String nodeId;
    private String studentNo;
    private String courseCode;
    private String roomType;
    private String result;
    private BigDecimal correctRate;
    private Integer hpLeft;
    private String answerSummaryJson;
    private String aiReportJson;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}

package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_ability_delta_log")
public class StudentAbilityDeltaLog {
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;
    private String studentNo;
    private String courseCode;
    private String runId;
    private String nodeId;
    private String knowledgePointId;
    private String abilityPointId;
    private Integer deltaScore;
    private Integer beforeScore;
    private Integer afterScore;
    private String reason;
    private String aiSummary;
    private LocalDateTime createdAt;
}

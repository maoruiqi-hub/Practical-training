package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_ability_snapshot")
public class StudentAbilitySnapshot {
    @TableId(value = "snapshot_id", type = IdType.ASSIGN_UUID)
    private String snapshotId;
    private String evaluationId;
    private String studentNo;
    private String courseCode;
    private String runId;
    private String nodeId;
    private String phase;
    private String abilityPointId;
    private String abilityPointName;
    private Integer score;
    private Integer evidenceKnowledgeCount;
    private Integer totalKnowledgeCount;
    private String knowledgePointIdsJson;
    private String weightsJson;
    private LocalDateTime createdAt;
}

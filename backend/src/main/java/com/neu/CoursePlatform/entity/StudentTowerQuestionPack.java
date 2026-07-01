package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_tower_question_pack")
public class StudentTowerQuestionPack {
    @TableId(value = "pack_id", type = IdType.ASSIGN_UUID)
    private String packId;
    private String runId;
    private String nodeId;
    private String studentNo;
    private String courseCode;
    private String mode;
    private String questionIdsJson;
    private String source;
    private String strategyJson;
    private String aiReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

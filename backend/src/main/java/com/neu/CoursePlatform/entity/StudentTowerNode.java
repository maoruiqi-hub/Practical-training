package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_tower_node")
public class StudentTowerNode {
    @TableId(value = "node_id", type = IdType.ASSIGN_UUID)
    private String nodeId;
    private String runId;
    private Integer nodeOrder;
    private Integer rowNo;
    private Integer colNo;
    private String roomType;
    private String status;
    private String knowledgePointId;
    private String abilityPointId;
    private String parentNodeId;
    private String unlockAfterNodeId;
    private Integer difficulty;
    private String aiReason;
    private String payloadJson;
    private LocalDateTime clearedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

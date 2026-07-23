package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_mastery_history")
public class KnowledgeMasteryHistory {
    @TableId(value = "history_id", type = IdType.ASSIGN_UUID)
    private String historyId;
    private String evidenceId;
    private String studentNo;
    private String courseCode;
    private String knowledgePointId;
    private Integer beforeScore;
    private Integer afterScore;
    private Integer targetScore;
    private BigDecimal alpha;
    private String formulaVersion;
    private LocalDateTime createdAt;
}

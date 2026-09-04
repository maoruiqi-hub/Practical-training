package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tower_action_log")
public class TowerActionLog {
    @TableId(value = "action_id", type = IdType.INPUT)
    private String actionId;
    private String runId;
    private String nodeId;
    private String studentNo;
    private String actionType;
    private String targetId;
    private String resultJson;
    private LocalDateTime createdAt;
}

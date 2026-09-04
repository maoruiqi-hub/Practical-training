package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tower_node_option")
public class TowerNodeOption {
    @TableId(value = "option_id", type = IdType.INPUT)
    private String optionId;
    private String runId;
    private String nodeId;
    private String optionKind;
    private String optionCode;
    private String optionSnapshotJson;
    private Boolean selected;
    private LocalDateTime createdAt;
    private LocalDateTime selectedAt;
}

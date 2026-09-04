package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tower_run_inventory")
public class TowerRunInventory {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String runId;
    private String studentNo;
    private String itemCode;
    private Integer quantity;
    private Integer version;
    private LocalDateTime updatedAt;
}

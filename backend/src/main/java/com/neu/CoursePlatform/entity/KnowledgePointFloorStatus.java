package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 模块一与模块四之间的楼层解锁回调状态。 */
@Data
@TableName("knowledge_point_floor_status")
public class KnowledgePointFloorStatus {
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;
    private String studentId;
    private String courseId;
    private String knowledgePointId;
    private String status;
    private LocalDateTime clearedAt;
    private LocalDateTime updatedAt;
}

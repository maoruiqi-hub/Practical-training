package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** The module-1 persistence side of a student's tower floor state. */
@Data
@TableName("knowledge_point_floor_status")
public class KnowledgePointFloorStatus {

    @TableId(value = "floor_status_id", type = IdType.AUTO)
    private String floorStatusId;
    private String studentNo;
    private String courseCode;
    private String knowledgePointId;
    /** locked, available, cleared or weak */
    private String status;
    private LocalDateTime updatedAt;
}

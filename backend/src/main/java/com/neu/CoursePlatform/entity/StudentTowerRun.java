package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_tower_run")
public class StudentTowerRun {
    @TableId(value = "run_id", type = IdType.ASSIGN_UUID)
    private String runId;
    private String studentNo;
    private String courseCode;
    private Integer version;
    private String status;
    private String routeSource;
    private String currentNodeId;
    private String aiSnapshotJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

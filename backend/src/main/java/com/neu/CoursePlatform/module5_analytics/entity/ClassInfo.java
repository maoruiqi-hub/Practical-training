package com.neu.CoursePlatform.module5_analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 班级实体 — 模块5 核心聚合单元
 */
@Data
@NoArgsConstructor
@TableName("analytics_class")
public class ClassInfo {

    /** 班级ID（UUID v4） */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 班级名称 */
    private String name;

    /** 关联课程ID（→ 模块1.Course） */
    private String courseId;

    /** 授课教师ID（→ Teacher） */
    private String teacherId;

    /** 学期（如 "2025-2026-1"） */
    private String semester;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最后修改时间 */
    private LocalDateTime updatedAt;
}

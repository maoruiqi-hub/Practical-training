package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习行为日志实体
 */
@Data
@NoArgsConstructor
public class LearningBehaviorLog {
    /** 日志编号 */
    @TableId(type = IdType.AUTO)
    private String logId;
    /** 用户ID */
    private String userId;
    /** 用户类型：student / teacher */
    private String userType;
    /** 资源类型：video / ppt / pdf / quiz / report / download */
    private String resourceType;
    /** 资源ID */
    private String resourceId;
    /** 关联任务编号 */
    private String taskNo;
    /** 关联知识点 */
    private String knowledgePoint;
    /** 动作类型：play / pause / seek / complete / view / submit / download */
    private String actionType;
    /** 开始时间 */
    private LocalDateTime startTime;
    /** 停留时长（秒） */
    private Integer duration;
    /** 完成状态：completed / partial / abandoned */
    private String completionStatus;
    /** 操作结果 */
    private String result;
    /** 创建时间 */
    private LocalDateTime createdAt;
}

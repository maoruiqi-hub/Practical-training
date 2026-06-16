package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习任务实体
 */
@Data
@NoArgsConstructor
public class LearningTask {
    /** 任务编号 */
    @TableId(type = IdType.AUTO)
    private String taskNo;
    /** 所属课程编号 */
    private String courseCode;
    /** 任务类型 */
    private String taskType;
    /** 任务说明 */
    private String description;
    /** 截止时间 */
    private LocalDateTime deadline;
    /** 提交方式 */
    private String submitMethod;
    /** 任务分数 */
    private Integer score;
}

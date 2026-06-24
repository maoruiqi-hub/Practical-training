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
    /** 任务名称 */
    private String taskName;
    /** 关联章节编号 */
    private String lessonNo;
    /** 关联知识点 JSON数组 */
    private String knowledgePoints;
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
    /** 评分规则 */
    private String gradingRule;
    /** 任务状态：draft/published/closed */
    private String status;
    /** 是否允许逾期提交：0/1 */
    private Integer allowLate;
    /** 最大提交次数 */
    private Integer maxAttempts;
    /** 允许的附件格式（逗号分隔） */
    private String attachmentFormats;
    /** 附件资源路径 */
    private String resourceUrl;
}

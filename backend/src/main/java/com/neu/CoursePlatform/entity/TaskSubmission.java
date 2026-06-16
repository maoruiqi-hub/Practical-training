package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务提交记录实体
 */
@Data
@NoArgsConstructor
public class TaskSubmission {
    /** 提交编号 */
    @TableId(type = IdType.AUTO)
    private String submissionId;
    /** 关联任务编号 */
    private String taskNo;
    /** 提交学生 */
    private String studentNo;
    /** 文字提交内容 */
    private String content;
    /** 上传文件路径 */
    private String filePath;
    /** 提交时间 */
    private LocalDateTime submitTime;
    /** 得分 */
    private Integer score;
    /** 状态：submitted 已提交 / graded 已批改 / returned 已打回 */
    private String status;
    /** 教师反馈 */
    private String feedback;
}

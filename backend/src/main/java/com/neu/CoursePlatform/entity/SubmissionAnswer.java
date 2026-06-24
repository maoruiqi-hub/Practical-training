package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 在线测验逐题作答明细
 */
@Data
@NoArgsConstructor
public class SubmissionAnswer {
    @TableId(type = IdType.AUTO)
    private String id;
    /** 关联提交记录 */
    private String submissionId;
    /** 关联任务 */
    private String taskNo;
    /** 提交学生 */
    private String studentNo;
    /** 关联题目 */
    private String questionId;
    /** 题干快照 */
    private String questionStem;
    /** 题型 */
    private String questionType;
    /** 知识点实体 ID 快照 */
    private String knowledgePointId;
    /** 学生答案 */
    private String studentAnswer;
    /** 正确答案或参考答案快照 */
    private String correctAnswer;
    /** 客观题是否正确，主观题为空 */
    private Boolean correct;
    /** 本题得分 */
    private Integer score;
    /** 本题满分 */
    private Integer maxScore;
    /** 是否可系统自动评阅 */
    private Boolean autoGradable;
    /** 创建时间 */
    private LocalDateTime createTime;
}

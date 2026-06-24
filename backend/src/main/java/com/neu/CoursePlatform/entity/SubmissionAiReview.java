package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 主观提交 AI 辅助评价结果
 */
@Data
@NoArgsConstructor
public class SubmissionAiReview {
    @TableId(type = IdType.AUTO)
    private String reviewId;
    private String submissionId;
    private String taskNo;
    private String studentNo;
    private Integer aiScore;
    private String dimensions;
    private String summary;
    private String suggestions;
    private String riskLevel;
    private String status;
    private LocalDateTime createTime;
}

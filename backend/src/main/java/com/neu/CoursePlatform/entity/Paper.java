package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 试卷版本实体
 */
@Data
@NoArgsConstructor
public class Paper {
    @TableId(type = IdType.AUTO)
    private String paperId;
    private String courseCode;
    private String taskNo;
    private String title;
    private String strategy;
    private Integer targetCount;
    private Integer totalScore;
    private String status;
    private LocalDateTime createTime;
}

package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 试卷版本实体
 */
@Data
@NoArgsConstructor
@TableName("exam")
public class Exam {
    @TableId(type = IdType.AUTO)
    private String examId;
    private String courseCode;
    private String taskNo;
    private String title;
    private String generateType;
    private Integer targetCount;
    private Integer totalScore;
    private String status;
    private LocalDateTime createTime;
}

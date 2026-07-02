package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 测验-题目关联实体
 */
@Data
@NoArgsConstructor
public class TaskQuestion {
    @TableId(type = IdType.AUTO)
    private String id;
    /** 关联 LearningTask.taskNo */
    private String taskNo;
    /** 关联 Question.questionId */
    private String questionId;
}

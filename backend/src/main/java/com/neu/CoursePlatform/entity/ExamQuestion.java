package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 试卷题目快照
 */
@Data
@NoArgsConstructor
@TableName("exam_question")
public class ExamQuestion {
    @TableId(type = IdType.AUTO)
    private String id;
    private String examId;
    private String questionId;
    private Integer sortOrder;
    private Integer scoreSnapshot;
    private String questionType;
    private String knowledgePointId;
    private Integer difficulty;
}

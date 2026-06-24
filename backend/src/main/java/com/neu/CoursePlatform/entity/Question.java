package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 题库实体
 */
@Data
@NoArgsConstructor
public class Question {
    @TableId(type = IdType.AUTO)
    private String questionId;
    /** 所属课程 */
    private String courseCode;
    /** 关联课时 */
    private String lessonNo;
    /** 题型：single / multi / fill / essay */
    private String type;
    /** 题干 */
    private String stem;
    /** 选项 JSON，填空和简答为 null */
    private String options;
    /** 正确答案 */
    private String answer;
    /** 难度 1~5 */
    private Integer difficulty;
    /** 关联知识点实体 ID */
    private String knowledgePointId;
    /** 默认分值 */
    private Integer score;
}

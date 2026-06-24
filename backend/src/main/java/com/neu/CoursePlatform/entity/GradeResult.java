package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("grade_result")
public class GradeResult {
    @TableId(value = "grade_result_id", type = IdType.AUTO)
    private String gradeResultId;
    private String targetId;
    private String targetType;
    private String courseCode;
    private String studentNo;
    private Integer score;
    private String feedback;
    private String gradedBy;
    private LocalDateTime createTime;
    private LocalDateTime updatedAt;
}

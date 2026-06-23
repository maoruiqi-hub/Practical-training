package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 课程实体
 */
@Data
@NoArgsConstructor
public class Course {
    /** 课程名称 */
    private String courseName;
    /** 课程编号 */
    @TableId(type = IdType.AUTO)
    private String courseCode;
    /** 授课教师 */
    private String teacher;
    /** 学分 */
    private Integer credits;
    /** 总学时 */
    private Integer hours;
    /** 封面图片URL */
    private String coverUrl;
    /** 课时列表 */
    @TableField(exist = false)
    private List<Lesson> lessons;
}

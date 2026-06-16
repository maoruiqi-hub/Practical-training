package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 课时实体（网课章节）
 */
@Data
@NoArgsConstructor
public class Lesson {
    /** 所属课程编号 */
    private String courseCode;
    /** 课时编号 */
    @TableId(type = IdType.AUTO)
    private String lessonNo;
    /** 课时标题 */
    private String lessonTitle;
    /** 资源类型：video / ppt / doc */
    private String resourceType;
    /** 资源地址 */
    private String resourceUrl;
    /** 内容简介 */
    private String description;
}

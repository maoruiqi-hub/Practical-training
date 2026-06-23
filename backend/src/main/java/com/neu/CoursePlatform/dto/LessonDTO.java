package com.neu.CoursePlatform.dto;

import lombok.Data;

/**
 * 课时详情 DTO（含课程名、教师名）
 */
@Data
public class LessonDTO {
    private String lessonNo;
    private String courseCode;
    private String courseName;
    private String teacherName;
    private String lessonTitle;
    private String resourceType;
    private String resourceUrl;
    private String description;
}

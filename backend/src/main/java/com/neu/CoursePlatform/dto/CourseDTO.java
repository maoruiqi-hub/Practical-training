package com.neu.CoursePlatform.dto;

import lombok.Data;

@Data
public class CourseDTO {
    private String courseCode;
    private String courseName;
    private String teacher;
    private Integer credits;
    private Integer hours;
    private String coverUrl;
    private String description;
    private String applicableMajor;
    private String courseObjectives;
    private int lessonCount;
}

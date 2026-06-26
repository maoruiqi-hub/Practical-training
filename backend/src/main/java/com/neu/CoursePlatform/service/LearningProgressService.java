package com.neu.CoursePlatform.service;

import java.util.Map;

public interface LearningProgressService {

    /** 学生课程进度 */
    Map<String, Object> buildStudentProgress(String studentNo, String courseCode);

    /** 教师查看全班进度 */
    Map<String, Object> buildCourseProgress(String courseCode);
}

package com.neu.CoursePlatform.service;

import java.util.Map;

public interface StatsService {

    Map<String, Object> buildStudentStats(String studentNo);

    Map<String, Object> buildCourseStats(String courseCode);

    /** 获取学生在某课程中的详细统计 */
    Map<String, Object> buildStudentCourseStats(String studentNo, String courseCode);
}

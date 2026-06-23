package com.neu.CoursePlatform.service;

import java.util.Map;

public interface StatsService {

    Map<String, Object> buildStudentStats(String studentNo);

    Map<String, Object> buildCourseStats(String courseCode);
}

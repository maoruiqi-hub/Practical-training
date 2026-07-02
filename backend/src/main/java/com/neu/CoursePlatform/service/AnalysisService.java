package com.neu.CoursePlatform.service;

import java.util.Map;

public interface AnalysisService {

    Map<String, Object> buildStudentWrongStats(String studentNo, String taskNo, String knowledgePointId, String type);

    Map<String, Object> buildTaskWrongStats(String taskNo);

    Map<String, Object> buildCourseWrongStats(String courseCode);
}

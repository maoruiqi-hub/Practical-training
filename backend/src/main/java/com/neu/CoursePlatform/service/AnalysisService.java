package com.neu.CoursePlatform.service;

import java.util.Map;

public interface AnalysisService {

    Map<String, Object> buildStudentWrongStats(String studentNo, String taskNo, String knowledgePointId, String type);

    default Map<String, Object> buildStudentWrongStats(String studentNo, String courseCode, String taskNo, String knowledgePointId, String type) {
        return buildStudentWrongStats(studentNo, taskNo, knowledgePointId, type);
    }

    Map<String, Object> buildTaskWrongStats(String taskNo);

    Map<String, Object> buildCourseWrongStats(String courseCode);
}

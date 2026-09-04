package com.neu.CoursePlatform.service;

import java.util.List;
import java.util.Map;

public interface StudentAbilityProjectionService {
    String FORMULA_VERSION = "knowledge_mastery_weighted_v1";

    List<Map<String, Object>> coursePoints(String studentNo, String courseCode);

    List<Map<String, Object>> trueCompetencies(String studentNo, String courseCode);
}

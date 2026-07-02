package com.neu.CoursePlatform.service;

import java.util.Map;

public interface AbilityRadarService {
    Map<String, Object> getAbilityRadar(String studentNo, String courseCode, String runId, String nodeId);
}

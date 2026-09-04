package com.neu.CoursePlatform.service;

import java.util.List;
import java.util.Map;

public interface TowerRunService {
    Map<String, Object> getOrCreateActiveRun(String studentNo, String courseCode);
    Map<String, Object> generateRun(String studentNo, String courseCode, boolean force);
    Map<String, Object> getNode(String studentNo, String runId, String nodeId);
    Map<String, Object> enterNode(String studentNo, String runId, String nodeId);
    Map<String, Object> completeNode(String studentNo, String runId, String nodeId, Map<String, Object> request);
    Map<String, Object> diagnoseNode(String studentNo, String runId, String nodeId, Map<String, Object> request);
    Map<String, Object> completeNonCombatNode(String studentNo, String runId, String nodeId, String result);
    Map<String, Object> getAttemptReport(String studentNo, String evaluationId);
    List<Map<String, Object>> getAbilityDeltas(String studentNo, String courseCode, String runId);
}

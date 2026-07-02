package com.neu.CoursePlatform.service;

import java.util.Map;

public interface TowerQuestionPackService {
    Map<String, Object> getOrCreateQuestionPack(String studentNo, String runId, String nodeId, String mode);
    Map<String, Object> regenerateQuestionPack(String studentNo, String runId, String nodeId, String mode);
}

package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.entity.StudentAbilitySnapshot;

import java.util.List;

public interface AbilitySnapshotService {
    List<StudentAbilitySnapshot> createBeforeSnapshots(String evaluationId, String studentNo, String courseCode,
                                                       String runId, String nodeId);

    List<StudentAbilitySnapshot> createAfterSnapshots(String evaluationId);

    List<AbilityScore> currentScores(String studentNo, String courseCode);

    record AbilityScore(String abilityPointId, String name, String description, int score,
                        int evidenceKnowledgeCount, int totalKnowledgeCount,
                        List<String> knowledgePointIds, List<Integer> weights) {}
}

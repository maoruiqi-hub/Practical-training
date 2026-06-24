package com.neu.CoursePlatform.profile.service;

import com.neu.CoursePlatform.profile.entity.CompetencyScore;
import com.neu.CoursePlatform.profile.entity.StudentProfile;
import java.util.*;

public interface ProfileService {
    StudentProfile getOrCreateProfile(Integer studentNo, Integer courseCode);
    void updateProfileFromSubmission(Integer studentNo, Integer courseCode,
                                    boolean correct, String taskType);
    List<CompetencyScore> getCompetencyScores(Integer studentNo, Integer courseCode);
    void updateCompetencyScores(Integer studentNo, Integer courseCode,
                               String abilityPointId, boolean correct);
    Map<String, Object> getProfileSummary(Integer studentNo, Integer courseCode);
    void addGrowth(Integer studentNo, Integer courseCode, int amount, String source, String sourceId);
    Map<String, Object> generateProfile(Integer studentNo, Integer courseCode);
    List<CompetencyScore> updateAllCompetencyScores(Integer studentNo, Integer courseCode);
    List<Map<String, Object>> getCompetencyHistory(Integer studentNo, Integer courseCode, String abilityPointId);
    List<Map<String, Object>> getGrowthHistory(Integer studentNo, Integer courseCode);
    Map<String, Object> generateTestFeedback(Integer studentNo, Integer courseCode);
}

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

    /** 教师端：获取课程下所有学生的画像摘要列表 */
    List<Map<String, Object>> listCourseStudentProfiles(Integer courseCode);

    /** 爬塔地图：获取学生视角的知识点楼层状态（§14.6） */
    List<Map<String, Object>> getTowerMap(Integer studentNo, Integer courseCode);

    // String-based cross-module API (conforms to VARCHAR(36) spec)
    default StudentProfile getOrCreateProfileStr(String studentNo, String courseCode) {
        return getOrCreateProfile(Integer.parseInt(studentNo), Integer.parseInt(courseCode));
    }
    default Map<String, Object> getProfileSummaryStr(String studentNo, String courseCode) {
        return getProfileSummary(Integer.parseInt(studentNo), Integer.parseInt(courseCode));
    }
    default void addGrowthStr(String studentNo, String courseCode, int amount, String source, String sourceId) {
        addGrowth(Integer.parseInt(studentNo), Integer.parseInt(courseCode), amount, source, sourceId);
    }
    default Map<String, Object> generateTestFeedbackStr(String studentNo, String courseCode) {
        return generateTestFeedback(Integer.parseInt(studentNo), Integer.parseInt(courseCode));
    }
    default List<CompetencyScore> getCompetencyScoresStr(String studentNo, String courseCode) {
        return getCompetencyScores(Integer.parseInt(studentNo), Integer.parseInt(courseCode));
    }
}

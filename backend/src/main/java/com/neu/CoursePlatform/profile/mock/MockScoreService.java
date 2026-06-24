package com.neu.CoursePlatform.profile.mock;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Mock for module 3's score records and mistake records.
 * When module 3 is implemented, replace with real service calls.
 */
@Component
public class MockScoreService {

    public List<Map<String, Object>> getStudentScores(Integer studentNo, Integer courseCode) {
        List<Map<String, Object>> scores = new ArrayList<>();
        Map<String, Object> s1 = new LinkedHashMap<>();
        s1.put("examName", "Python基础测验1");
        s1.put("score", 85);
        s1.put("totalScore", 100);
        s1.put("examDate", "2026-03-15");
        s1.put("rank", 5);
        s1.put("totalStudents", 30);
        scores.add(s1);

        Map<String, Object> s2 = new LinkedHashMap<>();
        s2.put("examName", "数据结构与算法测验");
        s2.put("score", 72);
        s2.put("totalScore", 100);
        s2.put("examDate", "2026-04-02");
        s2.put("rank", 12);
        s2.put("totalStudents", 28);
        scores.add(s2);

        Map<String, Object> s3 = new LinkedHashMap<>();
        s3.put("examName", "Python综合测验");
        s3.put("score", 90);
        s3.put("totalScore", 100);
        s3.put("examDate", "2026-05-10");
        s3.put("rank", 3);
        s3.put("totalStudents", 30);
        scores.add(s3);

        return scores;
    }

    public List<Map<String, Object>> getStudentMistakes(Integer studentNo, Integer courseCode) {
        List<Map<String, Object>> mistakes = new ArrayList<>();
        Map<String, Object> m1 = new LinkedHashMap<>();
        m1.put("questionId", "Q001");
        m1.put("knowledgePointId", "KP01");
        m1.put("abilityPointId", "AP01");
        m1.put("mistakeCount", 3);
        m1.put("lastWrongDate", "2026-04-10");
        m1.put("questionType", "单选");
        mistakes.add(m1);

        Map<String, Object> m2 = new LinkedHashMap<>();
        m2.put("questionId", "Q015");
        m2.put("knowledgePointId", "KP03");
        m2.put("abilityPointId", "AP02");
        m2.put("mistakeCount", 2);
        m2.put("lastWrongDate", "2026-05-01");
        m2.put("questionType", "编程");
        mistakes.add(m2);

        Map<String, Object> m3 = new LinkedHashMap<>();
        m3.put("questionId", "Q022");
        m3.put("knowledgePointId", "KP05");
        m3.put("abilityPointId", "AP03");
        m3.put("mistakeCount", 1);
        m3.put("lastWrongDate", "2026-05-15");
        m3.put("questionType", "多选");
        mistakes.add(m3);

        return mistakes;
    }
}

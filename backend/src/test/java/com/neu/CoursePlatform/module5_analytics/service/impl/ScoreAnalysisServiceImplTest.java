package com.neu.CoursePlatform.module5_analytics.service.impl;

import com.neu.CoursePlatform.module5_analytics.dto.ScoreOverviewDTO;
import com.neu.CoursePlatform.module5_analytics.dto.ScoreTrendDTO;
import com.neu.CoursePlatform.module5_analytics.dto.TaskCompletionDTO;
import com.neu.CoursePlatform.module5_analytics.dto.WeakPointDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.KnowledgePointDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.MistakeStatsDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentProgressDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentScoreDTO;
import com.neu.CoursePlatform.module5_analytics.service.external.ExternalDataProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreAnalysisServiceImplTest {

    // ======================== classScoreOverview ========================

    @Test
    void classOverviewForEmptyClassReturnsZeroStats() {
        ScoreAnalysisServiceImpl service = new ScoreAnalysisServiceImpl(new EmptyExternalDataProvider());

        ScoreOverviewDTO overview = service.getClassScoreOverview("empty-class", "course-1");

        assertEquals(0, overview.getAvgScore());
        assertEquals(0, overview.getMaxScore());
        assertEquals(0, overview.getMinScore());
        assertEquals(0, overview.getStdDev());
        assertEquals(0, overview.getPassRate());
        assertFalse(Double.isNaN(overview.getPassRate()));
        assertEquals(List.of(), overview.getDistribution());
        assertEquals(List.of(), overview.getRankings());
    }

    @Test
    void privateDistributionReturnsEmptyForEmptyScores() throws Exception {
        ScoreAnalysisServiceImpl service = new ScoreAnalysisServiceImpl(new EmptyExternalDataProvider());
        java.lang.reflect.Method method = ScoreAnalysisServiceImpl.class
                .getDeclaredMethod("buildDistribution", List.class);
        method.setAccessible(true);

        assertEquals(List.of(), method.invoke(service, List.of()));
    }

    @Test
    void classOverviewWithSingleStudentAllPass() {
        ExternalDataProvider provider = new FakeProvider()
                .addStudent("class-1", "S1", List.of(score("exam1", 85), score("exam2", 90), score("exam3", 88)));
        ScoreAnalysisServiceImpl service = new ScoreAnalysisServiceImpl(provider);

        ScoreOverviewDTO overview = service.getClassScoreOverview("class-1", "course-1");

        assertEquals(1, overview.getRankings().size());
        assertEquals(1, overview.getRankings().get(0).getRank());
        // avg = (85+90+88)/3 = 87.67
        assertEquals(87.67, overview.getAvgScore(), 0.01);
        assertEquals(100.0, overview.getPassRate(), 0.01);
        assertEquals(5, overview.getDistribution().size());
    }

    @Test
    void classOverviewWithMultipleStudentsMixedScores() {
        FakeProvider provider = new FakeProvider()
                .addStudent("class-2", "S1", List.of(score("e1", 95), score("e2", 92)))
                .addStudent("class-2", "S2", List.of(score("e1", 55), score("e2", 48)))
                .addStudent("class-2", "S3", List.of(score("e1", 72), score("e2", 78)));
        ScoreAnalysisServiceImpl service = new ScoreAnalysisServiceImpl(provider);

        ScoreOverviewDTO overview = service.getClassScoreOverview("class-2", "course-1");

        assertEquals(3, overview.getRankings().size());
        // Ranking: S1(93.5) rank1, S3(75) rank2, S2(51.5) rank3
        assertEquals(1, overview.getRankings().get(0).getRank());
        assertEquals("S1", overview.getRankings().get(0).getStudentId());
        assertEquals(3, overview.getRankings().get(2).getRank());
        assertEquals("S2", overview.getRankings().get(2).getStudentId());

        assertTrue(overview.getMaxScore() > 90);
        assertTrue(overview.getMinScore() < 55);
        // pass rate: 2/3 ≈ 66.67%
        assertEquals(66.67, overview.getPassRate(), 0.01);
        assertTrue(overview.getStdDev() > 0);
    }

    @Test
    void classOverviewAllFailBoundary() {
        FakeProvider provider = new FakeProvider()
                .addStudent("class-fail", "S1", List.of(score("e1", 30)))
                .addStudent("class-fail", "S2", List.of(score("e1", 59)));
        ScoreAnalysisServiceImpl service = new ScoreAnalysisServiceImpl(provider);

        ScoreOverviewDTO overview = service.getClassScoreOverview("class-fail", "course-1");

        assertEquals(0.0, overview.getPassRate(), 0.01);
        assertEquals(2, overview.getRankings().size());
    }

    @Test
    void classOverviewAllPassPerfectScores() {
        FakeProvider provider = new FakeProvider()
                .addStudent("class-perfect", "S1", List.of(score("e1", 100)))
                .addStudent("class-perfect", "S2", List.of(score("e1", 100)));
        ScoreAnalysisServiceImpl service = new ScoreAnalysisServiceImpl(provider);

        ScoreOverviewDTO overview = service.getClassScoreOverview("class-perfect", "course-1");

        assertEquals(100.0, overview.getPassRate(), 0.01);
        assertEquals(100.0, overview.getMaxScore(), 0.01);
        assertEquals(0.0, overview.getStdDev(), 0.01);
    }

    @Test
    void classOverviewStudentWithNoScores_TreatedAsZero() {
        FakeProvider provider = new FakeProvider()
                .addStudent("class-ns", "S1", List.of());
        ScoreAnalysisServiceImpl service = new ScoreAnalysisServiceImpl(provider);

        ScoreOverviewDTO overview = service.getClassScoreOverview("class-ns", "course-1");

        assertEquals(0.0, overview.getAvgScore(), 0.01);
        assertEquals(0.0, overview.getMaxScore(), 0.01);
    }

    // ======================== getWeakPoints ========================

    @Test
    void weakPointsWithKnownKnowledgePoints() {
        FakeProvider provider = new FakeProvider()
                .addKnowledgePoint("kp-1", "循环语句")
                .addKnowledgePoint("kp-2", "递归算法")
                .addMistakeStat(mistake("kp-1", "循环语句", 0.3, 100, 30))
                .addMistakeStat(mistake("kp-2", "递归算法", 0.5, 80, 40));
        ScoreAnalysisServiceImpl service = new ScoreAnalysisServiceImpl(provider);

        List<WeakPointDTO> weakPoints = service.getWeakPoints("course-1");

        assertEquals(2, weakPoints.size());
        // Lower scoreRate first
        assertEquals("递归算法", weakPoints.get(0).getKnowledgePointName());
        assertEquals(0.5, weakPoints.get(0).getScoreRate(), 0.01);
        assertEquals(0.7, weakPoints.get(1).getScoreRate(), 0.01);
    }

    @Test
    void weakPointsEmpty() {
        ScoreAnalysisServiceImpl service = new ScoreAnalysisServiceImpl(new EmptyExternalDataProvider());
        List<WeakPointDTO> weakPoints = service.getWeakPoints("course-1");
        assertTrue(weakPoints.isEmpty());
    }

    @Test
    void weakPointsFallsBackToNameWhenKpNotInCourse() {
        FakeProvider provider = new FakeProvider()
                .addKnowledgePoint("kp-a", "数组")
                .addMistakeStat(mistake("kp-x", "未知知识点", 0.2, 50, 10));
        ScoreAnalysisServiceImpl service = new ScoreAnalysisServiceImpl(provider);

        List<WeakPointDTO> weakPoints = service.getWeakPoints("course-1");
        assertEquals(1, weakPoints.size());
        assertEquals("未知知识点", weakPoints.get(0).getKnowledgePointName());
    }

    // ======================== getScoreTrends (class) ========================

    @Test
    void classTrendsSingleStudent() {
        FakeProvider provider = new FakeProvider()
                .addStudent("class-c", "S1", List.of(
                        score("w1", 60), score("w2", 70), score("w3", 80),
                        score("w4", 90), score("w5", 100)));
        ScoreAnalysisServiceImpl service = new ScoreAnalysisServiceImpl(provider);

        ScoreTrendDTO trends = service.getScoreTrends("class-c", "course-1", "week");

        assertEquals("week", trends.getGranularity());
        assertEquals(6, trends.getClassAvg().size());
        assertEquals("W1", trends.getClassAvg().get(0).getLabel());
        assertEquals(60.0, trends.getClassAvg().get(0).getValue(), 0.01);
    }

    @Test
    void classTrendsEmptyClass() {
        ScoreAnalysisServiceImpl service = new ScoreAnalysisServiceImpl(new EmptyExternalDataProvider());

        ScoreTrendDTO trends = service.getScoreTrends("empty", "course-1", null);

        assertEquals("week", trends.getGranularity()); // default
        assertTrue(trends.getClassAvg().isEmpty() || trends.getClassAvg().stream().allMatch(p -> p.getValue() == 0.0));
    }

    // ======================== getStudentScoreTrends ========================

    @Test
    void studentTrendsMultipleExams() {
        FakeProvider provider = new FakeProvider()
                .addStudent("class-s", "S1", List.of(
                        score("期中", 72), score("期末", 85)));
        ScoreAnalysisServiceImpl service = new ScoreAnalysisServiceImpl(provider);

        ScoreTrendDTO trends = service.getStudentScoreTrends("S1", "course-1", null);

        assertEquals("exam", trends.getGranularity());
        assertEquals(2, trends.getStudentScore().size());
        assertEquals("期中", trends.getStudentScore().get(0).getLabel());
    }

    @Test
    void studentTrendsNoScores() {
        FakeProvider provider = new FakeProvider()
                .addStudent("class-ns2", "S1", List.of());
        ScoreAnalysisServiceImpl service = new ScoreAnalysisServiceImpl(provider);

        ScoreTrendDTO trends = service.getStudentScoreTrends("S1", "course-1", null);

        assertTrue(trends.getStudentScore().isEmpty());
    }

    // ======================== distribution boundaries ========================

    @Test
    void distributionBoundaryScores() {
        // Test scores at each boundary exactly
        FakeProvider provider = new FakeProvider()
                .addStudent("class-d", "S1", List.of(score("e", 59)))  // 0-59
                .addStudent("class-d", "S2", List.of(score("e", 60)))  // 60-69
                .addStudent("class-d", "S3", List.of(score("e", 70)))  // 70-79
                .addStudent("class-d", "S4", List.of(score("e", 80)))  // 80-89
                .addStudent("class-d", "S5", List.of(score("e", 90))); // 90-100
        ScoreAnalysisServiceImpl service = new ScoreAnalysisServiceImpl(provider);

        ScoreOverviewDTO overview = service.getClassScoreOverview("class-d", "course-1");

        assertEquals(5, overview.getDistribution().size());
        assertEquals(1, overview.getDistribution().get(0).getCount()); // 0-59
        assertEquals(1, overview.getDistribution().get(4).getCount()); // 90-100
    }

    // ======================== helpers ========================

    private static StudentScoreDTO score(String targetId, double score) {
        StudentScoreDTO dto = new StudentScoreDTO();
        dto.setTargetId(targetId);
        dto.setScore(score);
        return dto;
    }

    private static MistakeStatsDTO mistake(String kpId, String kpName, double rate, int total, int mistakes) {
        MistakeStatsDTO dto = new MistakeStatsDTO();
        dto.setKnowledgePointId(kpId);
        dto.setKnowledgePointName(kpName);
        dto.setMistakeRate(rate);
        dto.setTotalAttempts(total);
        dto.setMistakeCount(mistakes);
        return dto;
    }

    // ======================== fake provider ========================

    private static class FakeProvider implements ExternalDataProvider {
        private final java.util.Map<String, java.util.Map<String, java.util.List<StudentScoreDTO>>> studentScores = new java.util.LinkedHashMap<>();
        private final java.util.Map<String, java.util.List<String>> classStudents = new java.util.LinkedHashMap<>();
        private final java.util.List<MistakeStatsDTO> mistakeStats = new java.util.ArrayList<>();
        private final java.util.List<KnowledgePointDTO> knowledgePoints = new java.util.ArrayList<>();

        FakeProvider addStudent(String classId, String studentId, List<StudentScoreDTO> scores) {
            classStudents.computeIfAbsent(classId, k -> new java.util.ArrayList<>()).add(studentId);
            studentScores.computeIfAbsent(studentId, k -> new java.util.LinkedHashMap<>()).put("course-1", scores);
            return this;
        }

        FakeProvider addMistakeStat(MistakeStatsDTO stat) { mistakeStats.add(stat); return this; }
        FakeProvider addKnowledgePoint(String id, String name) {
            KnowledgePointDTO dto = new KnowledgePointDTO();
            dto.setId(id); dto.setName(name);
            knowledgePoints.add(dto);
            return this;
        }

        @Override public List<StudentScoreDTO> getStudentScores(String studentId, String courseId) {
            return studentScores.getOrDefault(studentId, java.util.Map.of()).getOrDefault(courseId, List.of());
        }
        @Override public List<MistakeStatsDTO> getClassMistakeStats(String courseId) { return mistakeStats; }
        @Override public StudentProgressDTO getStudentProgress(String studentId, String courseId) { return null; }
        @Override public List<StudentProgressDTO> getClassProgressList(String classId, String courseId) { return List.of(); }
        @Override public TaskCompletionDTO getTaskCompletion(String classId, String taskId) { return null; }
        @Override public List<KnowledgePointDTO> getKnowledgePointsByCourse(String courseId) { return knowledgePoints; }
        @Override public List<String> getStudentIdsByClass(String classId) { return classStudents.getOrDefault(classId, List.of()); }
        @Override public LocalDateTime getLastActiveTime(String studentId) { return null; }
    }

    private static class EmptyExternalDataProvider implements ExternalDataProvider {
        @Override public List<StudentScoreDTO> getStudentScores(String studentId, String courseId) { return List.of(); }
        @Override public List<MistakeStatsDTO> getClassMistakeStats(String courseId) { return List.of(); }
        @Override public StudentProgressDTO getStudentProgress(String studentId, String courseId) { return null; }
        @Override public List<StudentProgressDTO> getClassProgressList(String classId, String courseId) { return List.of(); }
        @Override public TaskCompletionDTO getTaskCompletion(String classId, String taskId) { return null; }
        @Override public List<KnowledgePointDTO> getKnowledgePointsByCourse(String courseId) { return List.of(); }
        @Override public List<String> getStudentIdsByClass(String classId) { return List.of(); }
        @Override public LocalDateTime getLastActiveTime(String studentId) { return null; }
    }
}

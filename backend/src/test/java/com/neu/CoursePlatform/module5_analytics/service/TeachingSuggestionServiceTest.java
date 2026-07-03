package com.neu.CoursePlatform.module5_analytics.service;

import com.neu.CoursePlatform.agentic.AgenticClient;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeachingSuggestionServiceTest {

    @Test
    void generateForClassFallsBackToLearningDataWhenAgenticUnavailable() throws Exception {
        AgenticClient agenticClient = mock(AgenticClient.class);
        when(agenticClient.teachingSuggestions(anyMap()))
                .thenThrow(new AgenticClient.AgenticException("network unavailable"));

        RiskAlertService riskAlertService = mock(RiskAlertService.class);
        when(riskAlertService.getActiveByClass("class-1", List.of("S1", "S2"))).thenReturn(List.of());

        TeachingSuggestionService service = new TeachingSuggestionService(
                agenticClient,
                new FakeProvider(),
                new FakeScoreAnalysisService(),
                riskAlertService
        );

        List<Map<String, Object>> result = service.generateForClass("class-1", "course-1");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("fallback", result.get(0).get("source"));
        assertTrue(String.valueOf(result.get(0).get("content")).contains("循环语句"));
    }

    private static class FakeScoreAnalysisService implements ScoreAnalysisService {
        @Override
        public ScoreOverviewDTO getClassScoreOverview(String classId, String courseId) {
            return null;
        }

        @Override
        public List<WeakPointDTO> getWeakPoints(String courseId) {
            WeakPointDTO weakPoint = new WeakPointDTO();
            weakPoint.setKnowledgePointId("kp-1");
            weakPoint.setKnowledgePointName("循环语句");
            weakPoint.setScoreRate(0.45);
            weakPoint.setTotalAttempts(30);
            weakPoint.setMistakeCount(16);
            return List.of(weakPoint);
        }

        @Override
        public ScoreTrendDTO getScoreTrends(String classId, String courseId, String granularity) {
            return null;
        }

        @Override
        public ScoreTrendDTO getStudentScoreTrends(String studentId, String courseId, String granularity) {
            return null;
        }
    }

    private static class FakeProvider implements ExternalDataProvider {
        @Override
        public List<StudentScoreDTO> getStudentScores(String studentId, String courseId) {
            return List.of();
        }

        @Override
        public List<MistakeStatsDTO> getClassMistakeStats(String courseId) {
            return List.of();
        }

        @Override
        public StudentProgressDTO getStudentProgress(String studentId, String courseId) {
            return null;
        }

        @Override
        public List<StudentProgressDTO> getClassProgressList(String classId, String courseId) {
            StudentProgressDTO progress = new StudentProgressDTO();
            progress.setStudentId("S1");
            progress.setCompletionRate(0.72);
            return List.of(progress);
        }

        @Override
        public TaskCompletionDTO getTaskCompletion(String classId, String taskId) {
            return null;
        }

        @Override
        public List<KnowledgePointDTO> getKnowledgePointsByCourse(String courseId) {
            return List.of();
        }

        @Override
        public List<String> getStudentIdsByClass(String classId) {
            return List.of("S1", "S2");
        }

        @Override
        public LocalDateTime getLastActiveTime(String studentId) {
            return null;
        }
    }
}

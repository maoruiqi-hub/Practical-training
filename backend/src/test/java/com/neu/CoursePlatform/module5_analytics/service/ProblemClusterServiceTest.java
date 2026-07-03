package com.neu.CoursePlatform.module5_analytics.service;

import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.module5_analytics.dto.TaskCompletionDTO;
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

class ProblemClusterServiceTest {

    @Test
    void clusterFallsBackToMistakeStatsWhenAgenticUnavailable() throws Exception {
        AgenticClient agenticClient = mock(AgenticClient.class);
        when(agenticClient.clusterProblems(anyMap()))
                .thenThrow(new AgenticClient.AgenticException("network unavailable"));

        ProblemClusterService service = new ProblemClusterService(agenticClient, new FakeProvider());

        List<Map<String, Object>> result = service.cluster("class-1", "course-1");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("fallback", result.get(0).get("source"));
        assertTrue(String.valueOf(result.get(0).get("topic")).contains("递归"));
    }

    private static class FakeProvider implements ExternalDataProvider {
        @Override
        public List<StudentScoreDTO> getStudentScores(String studentId, String courseId) {
            return List.of();
        }

        @Override
        public List<MistakeStatsDTO> getClassMistakeStats(String courseId) {
            MistakeStatsDTO recursion = new MistakeStatsDTO();
            recursion.setKnowledgePointId("kp-2");
            recursion.setKnowledgePointName("递归算法");
            recursion.setMistakeRate(0.62);
            recursion.setTotalAttempts(20);
            recursion.setMistakeCount(12);
            return List.of(recursion);
        }

        @Override
        public StudentProgressDTO getStudentProgress(String studentId, String courseId) {
            return null;
        }

        @Override
        public List<StudentProgressDTO> getClassProgressList(String classId, String courseId) {
            return List.of();
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
            return List.of("S1", "S2", "S3");
        }

        @Override
        public LocalDateTime getLastActiveTime(String studentId) {
            return null;
        }
    }
}

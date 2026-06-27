package com.neu.CoursePlatform.module5_analytics.service.impl;

import com.neu.CoursePlatform.module5_analytics.dto.ScoreOverviewDTO;
import com.neu.CoursePlatform.module5_analytics.dto.TaskCompletionDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.KnowledgePointDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.MistakeStatsDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentProgressDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentScoreDTO;
import com.neu.CoursePlatform.module5_analytics.service.external.ExternalDataProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ScoreAnalysisServiceImplTest {

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

    private static class EmptyExternalDataProvider implements ExternalDataProvider {
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
            return List.of();
        }

        @Override
        public LocalDateTime getLastActiveTime(String studentId) {
            return null;
        }
    }
}

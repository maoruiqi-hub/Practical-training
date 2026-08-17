package com.neu.CoursePlatform.module5_analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.mapper.KnowledgeMasteryHistoryMapper;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentProgressDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentScoreDTO;
import com.neu.CoursePlatform.module5_analytics.entity.RiskAlert;
import com.neu.CoursePlatform.module5_analytics.service.external.ExternalDataProvider;
import com.neu.CoursePlatform.profile.mapper.CompetencyScoreHistoryMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RiskDetectionServiceTest {

    @Test
    void detectsSuddenDropAgainstChronologicalPersonalBaseline() {
        ExternalDataProvider dataProvider = mock(ExternalDataProvider.class);
        RiskAlertService alertService = mock(RiskAlertService.class);
        CompetencyScoreHistoryMapper abilityHistory = mock(CompetencyScoreHistoryMapper.class);
        KnowledgeMasteryHistoryMapper masteryHistory = mock(KnowledgeMasteryHistoryMapper.class);
        RiskDetectionService service = new RiskDetectionService(dataProvider, alertService,
                abilityHistory, masteryHistory, new ObjectMapper());

        when(dataProvider.getStudentScores("1001", "1")).thenReturn(List.of(
                score(46, 4), score(82, 1), score(79, 2), score(81, 3)));
        StudentProgressDTO progress = new StudentProgressDTO();
        progress.setCompletionRate(1D);
        when(dataProvider.getStudentProgress("1001", "1")).thenReturn(progress);
        when(dataProvider.getLastActiveTime("1001")).thenReturn(LocalDateTime.now());
        when(masteryHistory.selectList(any())).thenReturn(List.of());
        when(abilityHistory.selectList(any())).thenReturn(List.of());
        when(alertService.hasActiveAlert(anyString(), anyString())).thenReturn(false);
        when(alertService.receiveEvent(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new RiskAlert());

        service.detectForStudent("1001", "1", 1D);

        verify(alertService).receiveEvent(eq("1001"), eq("1"), eq("score_decline"), eq("high"),
                contains("-34.67"));
    }

    private StudentScoreDTO score(double value, int day) {
        StudentScoreDTO score = new StudentScoreDTO();
        score.setScore(value);
        score.setTotalScore(100D);
        score.setTargetId("quiz-" + day);
        score.setTargetType("quiz");
        score.setScoredAt(LocalDateTime.of(2026, 8, day, 10, 0));
        return score;
    }
}

package com.neu.CoursePlatform.module5_analytics.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.module5_analytics.dto.external.StudentProgressDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentScoreDTO;
import com.neu.CoursePlatform.module5_analytics.entity.RiskAlert;
import com.neu.CoursePlatform.module5_analytics.service.external.ExternalDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class RiskDetectionServiceTest {

    private ExternalDataProvider dataProvider;
    private RiskAlertService riskAlertService;
    private RiskDetectionService service;

    @BeforeEach
    void setUp() {
        dataProvider = mock(ExternalDataProvider.class);
        riskAlertService = mock(RiskAlertService.class);
        service = new RiskDetectionService(dataProvider, riskAlertService);

        when(riskAlertService.hasActiveAlert(anyString(), anyString())).thenReturn(false);
        when(riskAlertService.receiveEvent(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(inv -> RiskAlert.create(
                        inv.getArgument(0),
                        inv.getArgument(1),
                        inv.getArgument(2),
                        inv.getArgument(3),
                        inv.getArgument(4)));
    }

    @Test
    void detectForStudentCreatesAlertsForAllMatchedRules() {
        when(dataProvider.getLastActiveTime("s1")).thenReturn(LocalDateTime.now().minusDays(8));
        when(dataProvider.getStudentScores("s1", "c1")).thenReturn(List.of(
                score(82), score(68), score(55), score(50), score(45)));
        when(dataProvider.getStudentProgress("s1", "c1")).thenReturn(progress(0.20));

        List<RiskAlert> alerts = service.detectForStudent("s1", "c1", 0.70);

        Set<String> riskTypes = alerts.stream().map(RiskAlert::getRiskType).collect(Collectors.toSet());
        assertEquals(Set.of("procrastination", "low_score", "score_decline", "inactive", "progress_lag"), riskTypes);
        assertTrue(alerts.stream().anyMatch(alert ->
                "procrastination".equals(alert.getRiskType()) && "high".equals(alert.getRiskLevel())));
        assertTrue(alerts.stream().anyMatch(alert ->
                "inactive".equals(alert.getRiskType()) && alert.getDetail().contains("days_since_active")));
    }

    @Test
    void detectForStudentUsesMediumLevelsAndIgnoresMissingData() {
        when(dataProvider.getLastActiveTime("s1")).thenReturn(LocalDateTime.now().minusDays(4));
        when(dataProvider.getStudentScores("s1", "c1")).thenReturn(List.of(score(90), score(88)));
        when(dataProvider.getStudentProgress("s1", "c1")).thenReturn(null);

        List<RiskAlert> alerts = service.detectForStudent("s1", "c1", 0.50);

        Set<String> riskTypes = alerts.stream().map(RiskAlert::getRiskType).collect(Collectors.toSet());
        assertEquals(Set.of("procrastination", "inactive"), riskTypes);
        assertTrue(alerts.stream().allMatch(alert -> "medium".equals(alert.getRiskLevel())));
    }

    @Test
    void detectForStudentSkipsDuplicatedActiveAlerts() {
        when(dataProvider.getLastActiveTime("s1")).thenReturn(LocalDateTime.now().minusDays(8));
        when(dataProvider.getStudentScores("s1", "c1")).thenReturn(List.of(score(50), score(48), score(45)));
        when(dataProvider.getStudentProgress("s1", "c1")).thenReturn(progress(0.10));
        when(riskAlertService.hasActiveAlert(eq("s1"), anyString())).thenReturn(true);

        List<RiskAlert> alerts = service.detectForStudent("s1", "c1", 0.70);

        assertTrue(alerts.isEmpty());
        verify(riskAlertService, never()).receiveEvent(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void detectForClassComputesAverageProgressAndAggregatesStudents() {
        when(dataProvider.getStudentIdsByClass("class-1")).thenReturn(List.of("s1", "s2"));
        when(dataProvider.getClassProgressList("class-1", "c1")).thenReturn(List.of(progress(0.90), progress(0.50)));
        when(dataProvider.getLastActiveTime(anyString())).thenReturn(LocalDateTime.now());
        when(dataProvider.getStudentScores(anyString(), eq("c1"))).thenReturn(List.of(score(90), score(88), score(86)));
        when(dataProvider.getStudentProgress("s1", "c1")).thenReturn(progress(0.20));
        when(dataProvider.getStudentProgress("s2", "c1")).thenReturn(progress(0.65));

        List<RiskAlert> alerts = service.detectForClass("class-1", "c1");

        assertEquals(1, alerts.size());
        assertEquals("progress_lag", alerts.get(0).getRiskType());
        assertEquals("s1", alerts.get(0).getStudentId());
    }

    private static StudentScoreDTO score(double value) {
        StudentScoreDTO dto = new StudentScoreDTO();
        dto.setScore(value);
        return dto;
    }

    private static StudentProgressDTO progress(double rate) {
        StudentProgressDTO dto = new StudentProgressDTO();
        dto.setCompletionRate(rate);
        return dto;
    }
}

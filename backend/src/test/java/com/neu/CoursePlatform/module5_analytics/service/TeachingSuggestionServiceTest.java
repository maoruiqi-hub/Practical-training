package com.neu.CoursePlatform.module5_analytics.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.module5_analytics.dto.WeakPointDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentProgressDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentScoreDTO;
import com.neu.CoursePlatform.module5_analytics.entity.RiskAlert;
import com.neu.CoursePlatform.module5_analytics.service.external.ExternalDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

class TeachingSuggestionServiceTest {

    private AgenticClient agenticClient;
    private ExternalDataProvider dataProvider;
    private ScoreAnalysisService scoreAnalysisService;
    private RiskAlertService riskAlertService;
    private TeachingSuggestionService service;

    @BeforeEach
    void setUp() {
        agenticClient = mock(AgenticClient.class);
        dataProvider = mock(ExternalDataProvider.class);
        scoreAnalysisService = mock(ScoreAnalysisService.class);
        riskAlertService = mock(RiskAlertService.class);
        service = new TeachingSuggestionService(agenticClient, dataProvider, scoreAnalysisService, riskAlertService);
    }

    @Test
    void generateForClassBuildsContextAndParsesSuggestionsArray() throws AgenticClient.AgenticException {
        when(scoreAnalysisService.getWeakPoints("course-1")).thenReturn(List.of(weak("kp-1", "循环", 0.52)));
        when(dataProvider.getClassProgressList("class-1", "course-1")).thenReturn(List.of(progress("s1", 0.5), progress("s2", 1.0)));
        when(dataProvider.getStudentIdsByClass("class-1")).thenReturn(List.of("s1", "s2"));
        RiskAlert alert = new RiskAlert();
        alert.setRiskLevel("high");
        when(riskAlertService.getActiveByClass(eq("class-1"), anyList())).thenReturn(List.of(alert));
        when(agenticClient.teachingSuggestions(anyMap())).thenReturn("""
                [{"suggestion_type":"reteach","content":"重讲循环边界","target":"whole_class","urgency":"high"}]
                """);

        List<Map<String, Object>> result = service.generateForClass("class-1", "course-1");

        assertEquals(1, result.size());
        assertEquals("重讲循环边界", result.get(0).get("content"));
        assertEquals("high", result.get(0).get("urgency"));
        assertNotNull(result.get(0).get("generated_at"));
        verify(agenticClient).teachingSuggestions(argThat(request ->
                ((Number) ((Map<?, ?>) request.get("progress_data")).get("avg_completion_rate")).doubleValue() == 0.75
                        && ((Number) request.get("active_risk_count")).longValue() == 1));
    }

    @Test
    void generateForStudentUsesScoresProgressAndRiskLevel() throws AgenticClient.AgenticException {
        when(dataProvider.getStudentScores("s1", "course-1")).thenReturn(List.of(score(82D)));
        when(dataProvider.getStudentProgress("s1", "course-1")).thenReturn(progress("s1", 0.4));
        when(riskAlertService.getStudentRiskStatus("s1"))
                .thenReturn(new RiskAlertService.RiskStatus("s1", List.of(), "medium"));
        when(agenticClient.teachingSuggestions(anyMap())).thenReturn("""
                {"suggestions":[{"suggestion_type":"intervention","content":"安排一对一答疑"}]}
                """);

        List<Map<String, Object>> result = service.generateForStudent("s1", "course-1");

        assertEquals("intervention", result.get(0).get("suggestion_type"));
        assertEquals("安排一对一答疑", result.get(0).get("content"));
        assertNotNull(result.get(0).get("generated_at"));
    }

    @Test
    void generateForStudentHandlesNullProgress() throws AgenticClient.AgenticException {
        when(dataProvider.getStudentScores("s1", "course-1")).thenReturn(List.of(score(60D)));
        when(dataProvider.getStudentProgress("s1", "course-1")).thenReturn(null);
        when(riskAlertService.getStudentRiskStatus("s1"))
                .thenReturn(new RiskAlertService.RiskStatus("s1", List.of(), "low"));
        when(agenticClient.teachingSuggestions(anyMap())).thenReturn("{\"content\":\"保持跟进\"}");

        List<Map<String, Object>> result = service.generateForStudent("s1", "course-1");

        assertEquals("保持跟进", result.get(0).get("content"));
        verify(agenticClient).teachingSuggestions(argThat(request ->
                ((Number) ((Map<?, ?>) request.get("progress")).get("completion_rate")).doubleValue() == 0D));
    }

    @Test
    void generateReturnsNullWhenAgenticUnavailable() throws AgenticClient.AgenticException {
        when(scoreAnalysisService.getWeakPoints("course-1")).thenReturn(List.of());
        when(dataProvider.getClassProgressList("class-1", "course-1")).thenReturn(List.of());
        when(dataProvider.getStudentIdsByClass("class-1")).thenReturn(List.of());
        when(riskAlertService.getActiveByClass(anyString(), anyList())).thenReturn(List.of());
        when(agenticClient.teachingSuggestions(anyMap())).thenThrow(new AgenticClient.AgenticException("down"));

        assertNull(service.generateForClass("class-1", "course-1"));
    }

    @Test
    void generateFallsBackForUnstructuredTextAndHistoryIsEmpty() throws AgenticClient.AgenticException {
        when(scoreAnalysisService.getWeakPoints("course-1")).thenReturn(List.of());
        when(dataProvider.getClassProgressList("class-1", "course-1")).thenReturn(List.of());
        when(dataProvider.getStudentIdsByClass("class-1")).thenReturn(List.of());
        when(riskAlertService.getActiveByClass(anyString(), anyList())).thenReturn(List.of());
        when(agenticClient.teachingSuggestions(anyMap())).thenReturn("plain advice");

        List<Map<String, Object>> result = service.generateForClass("class-1", "course-1");

        assertEquals("reteach", result.get(0).get("suggestion_type"));
        assertEquals("plain advice", result.get(0).get("raw_response"));
        assertTrue(service.getHistory("class-1").isEmpty());
    }

    private WeakPointDTO weak(String kpId, String name, double rate) {
        WeakPointDTO dto = new WeakPointDTO();
        dto.setKnowledgePointId(kpId);
        dto.setKnowledgePointName(name);
        dto.setScoreRate(rate);
        return dto;
    }

    private StudentProgressDTO progress(String studentId, double completionRate) {
        StudentProgressDTO dto = new StudentProgressDTO();
        dto.setStudentId(studentId);
        dto.setTotalTasks(10);
        dto.setCompletedTasks((int) Math.round(completionRate * 10));
        dto.setCompletionRate(completionRate);
        return dto;
    }

    private StudentScoreDTO score(Double value) {
        StudentScoreDTO dto = new StudentScoreDTO();
        dto.setScore(value);
        dto.setScoredAt(LocalDateTime.now());
        return dto;
    }
}

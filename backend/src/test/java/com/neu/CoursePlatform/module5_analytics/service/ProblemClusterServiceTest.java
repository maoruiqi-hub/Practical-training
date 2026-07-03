package com.neu.CoursePlatform.module5_analytics.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.module5_analytics.dto.external.MistakeStatsDTO;
import com.neu.CoursePlatform.module5_analytics.service.external.ExternalDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class ProblemClusterServiceTest {

    private AgenticClient agenticClient;
    private ExternalDataProvider dataProvider;
    private ProblemClusterService service;

    @BeforeEach
    void setUp() {
        agenticClient = mock(AgenticClient.class);
        dataProvider = mock(ExternalDataProvider.class);
        service = new ProblemClusterService(agenticClient, dataProvider);
    }

    @Test
    void clusterParsesArrayResponseAndAddsGeneratedAt() throws AgenticClient.AgenticException {
        when(dataProvider.getStudentIdsByClass("class-1")).thenReturn(List.of("s1", "s2"));
        when(dataProvider.getClassMistakeStats("course-1")).thenReturn(List.of(mistake("kp-1", "循环", 0.6)));
        when(agenticClient.clusterProblems(anyMap())).thenReturn("""
                ```json
                [{"topic":"循环边界错误","student_count":2,"knowledge_point_id":"kp-1"}]
                ```
                """);

        List<Map<String, Object>> result = service.cluster("class-1", "course-1");

        assertEquals(1, result.size());
        assertEquals("循环边界错误", result.get(0).get("topic"));
        assertEquals("kp-1", result.get(0).get("knowledge_point_id"));
        assertNotNull(result.get(0).get("generated_at"));
        verify(agenticClient).clusterProblems(argThat(request -> {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> mistakes = (List<Map<String, Object>>) request.get("mistakes");
            return mistakes.size() == 2;
        }));
    }

    @Test
    void clusterParsesObjectWrappedItems() throws AgenticClient.AgenticException {
        when(dataProvider.getStudentIdsByClass("class-1")).thenReturn(List.of("s1"));
        when(dataProvider.getClassMistakeStats("course-1")).thenReturn(List.of(mistake("kp-1", "函数", 0.4)));
        when(agenticClient.clusterProblems(anyMap())).thenReturn("""
                {"clusters":[{"topic":"函数参数混淆","student_count":1}]}
                """);

        List<Map<String, Object>> result = service.cluster("class-1", "course-1");

        assertEquals("函数参数混淆", result.get(0).get("topic"));
        assertNotNull(result.get(0).get("generated_at"));
    }

    @Test
    void clusterReturnsNullWhenAgenticUnavailable() throws AgenticClient.AgenticException {
        when(dataProvider.getStudentIdsByClass("class-1")).thenReturn(List.of("s1"));
        when(dataProvider.getClassMistakeStats("course-1")).thenReturn(List.of(mistake("kp-1", "函数", 0.4)));
        when(agenticClient.clusterProblems(anyMap())).thenThrow(new AgenticClient.AgenticException("down"));

        assertNull(service.cluster("class-1", "course-1"));
    }

    @Test
    void clusterFallsBackToRawResponseWhenJsonCannotBeAdapted() throws AgenticClient.AgenticException {
        when(dataProvider.getStudentIdsByClass("class-1")).thenReturn(List.of("s1"));
        when(dataProvider.getClassMistakeStats("course-1")).thenReturn(List.of());
        when(agenticClient.clusterProblems(anyMap())).thenReturn("plain text response");

        List<Map<String, Object>> result = service.cluster("class-1", "course-1");

        assertEquals("聚类结果", result.get(0).get("topic"));
        assertEquals("plain text response", result.get(0).get("raw_response"));
    }

    @Test
    void getLatestClusterReturnsEmptyListInCurrentPhase() {
        assertTrue(service.getLatestCluster("class-1").isEmpty());
    }

    private MistakeStatsDTO mistake(String kpId, String name, double rate) {
        MistakeStatsDTO dto = new MistakeStatsDTO();
        dto.setKnowledgePointId(kpId);
        dto.setKnowledgePointName(name);
        dto.setMistakeRate(rate);
        dto.setMistakeCount(3);
        dto.setTotalAttempts(5);
        return dto;
    }
}

package com.neu.CoursePlatform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.entity.StudentAbilityDeltaLog;
import com.neu.CoursePlatform.mapper.StudentAbilityDeltaLogMapper;
import com.neu.CoursePlatform.profile.entity.CompetencyScore;
import com.neu.CoursePlatform.profile.service.ProfileService;
import com.neu.CoursePlatform.service.AbilityPointService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

class AbilityRadarServiceImplTest {

    private AbilityRadarServiceImpl service;
    private AbilityPointService abilityPointService;
    private ProfileService profileService;
    private StudentAbilityDeltaLogMapper deltaMapper;

    @BeforeEach
    void setUp() {
        abilityPointService = mock(AbilityPointService.class);
        profileService = mock(ProfileService.class);
        deltaMapper = mock(StudentAbilityDeltaLogMapper.class);
        service = new AbilityRadarServiceImpl(abilityPointService, profileService, deltaMapper);
    }

    // ============ getAbilityRadar ============

    @Test
    void getAbilityRadarReturnsCurrentModeWhenNoRunId() {
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(ability("ab-1", "逻辑思维", "逻辑能力")));
        when(profileService.getCompetencyScores(anyInt(), anyInt())).thenReturn(List.of());
        when(deltaMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = service.getAbilityRadar("2024001", "101", null, null);

        assertEquals("current", result.get("mode"));
        assertNotNull(result.get("dimensions"));
        assertNotNull(result.get("series"));
        assertNotNull(result.get("summary"));
    }

    @Test
    void getAbilityRadarReturnsNodeModeWhenNodeIdGiven() {
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(ability("ab-1", "逻辑思维", "")));
        when(profileService.getCompetencyScores(anyInt(), anyInt())).thenReturn(List.of());
        when(deltaMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = service.getAbilityRadar("2024001", "101", "r1", "n1");

        assertEquals("node", result.get("mode"));
        assertEquals("r1", result.get("runId"));
        assertEquals("n1", result.get("nodeId"));
    }

    @Test
    void getAbilityRadarReturnsRunModeWhenRunIdGivenButNoNodeId() {
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(ability("ab-1", "逻辑思维", "")));
        when(profileService.getCompetencyScores(anyInt(), anyInt())).thenReturn(List.of());
        when(deltaMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = service.getAbilityRadar("2024001", "101", "r1", null);

        assertEquals("run", result.get("mode"));
    }

    @Test
    void getAbilityRadarIncludesCurrentScoresFromProfile() {
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(ability("ab-1", "逻辑思维", "")));
        CompetencyScore score = new CompetencyScore();
        score.setAbilityPointId("ab-1");
        score.setScore(75);
        when(profileService.getCompetencyScores(anyInt(), anyInt())).thenReturn(List.of(score));
        when(deltaMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = service.getAbilityRadar("2024001", "101", null, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dimensions = (List<Map<String, Object>>) result.get("dimensions");
        assertEquals(1, dimensions.size());
        assertEquals(75, dimensions.get(0).get("beforeScore"));
        assertEquals(75, dimensions.get(0).get("afterScore"));
    }

    @Test
    void getAbilityRadarHandlesProfileServiceException() {
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(ability("ab-1", "逻辑思维", "")));
        when(profileService.getCompetencyScores(anyInt(), anyInt())).thenThrow(new RuntimeException("DB error"));
        when(deltaMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = service.getAbilityRadar("2024001", "101", null, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dimensions = (List<Map<String, Object>>) result.get("dimensions");
        assertEquals(50, dimensions.get(0).get("beforeScore")); // default 50
    }

    @Test
    void getAbilityRadarUsesDeltaLogsForBeforeAfterScores() {
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(ability("ab-1", "逻辑思维", "")));
        when(profileService.getCompetencyScores(anyInt(), anyInt())).thenReturn(List.of());

        StudentAbilityDeltaLog log = new StudentAbilityDeltaLog();
        log.setAbilityPointId("ab-1");
        log.setBeforeScore(60);
        log.setAfterScore(80);
        log.setCreatedAt(LocalDateTime.now());
        when(deltaMapper.selectList(any())).thenReturn(List.of(log));

        Map<String, Object> result = service.getAbilityRadar("2024001", "101", null, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dimensions = (List<Map<String, Object>>) result.get("dimensions");
        assertEquals(60, dimensions.get(0).get("beforeScore"));
        assertEquals(80, dimensions.get(0).get("afterScore"));
        assertEquals(20, dimensions.get(0).get("delta"));
    }

    @Test
    void getAbilityRadarClampsScoresToRange() {
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(ability("ab-1", "逻辑思维", "")));
        CompetencyScore score = new CompetencyScore();
        score.setAbilityPointId("ab-1");
        score.setScore(150); // over 100
        when(profileService.getCompetencyScores(anyInt(), anyInt())).thenReturn(List.of(score));
        when(deltaMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = service.getAbilityRadar("2024001", "101", null, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dimensions = (List<Map<String, Object>>) result.get("dimensions");
        assertEquals(100, dimensions.get(0).get("beforeScore"));
        assertEquals(100, dimensions.get(0).get("afterScore"));
    }

    @Test
    void getAbilityRadarClampsNegativeScoresToZero() {
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(ability("ab-1", "逻辑思维", "")));
        CompetencyScore score = new CompetencyScore();
        score.setAbilityPointId("ab-1");
        score.setScore(-10);
        when(profileService.getCompetencyScores(anyInt(), anyInt())).thenReturn(List.of(score));
        when(deltaMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = service.getAbilityRadar("2024001", "101", null, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dimensions = (List<Map<String, Object>>) result.get("dimensions");
        assertEquals(0, dimensions.get(0).get("beforeScore"));
    }

    @Test
    void getAbilityRadarSummaryWhenNoChanges() {
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(ability("ab-1", "逻辑思维", "")));
        when(profileService.getCompetencyScores(anyInt(), anyInt())).thenReturn(List.of());
        when(deltaMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = service.getAbilityRadar("2024001", "101", null, null);

        String summary = (String) result.get("summary");
        assertTrue(summary.contains("没有"));
    }

    @Test
    void getAbilityRadarSummaryWhenChangesExist() {
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(ability("ab-1", "逻辑思维", "")));
        when(profileService.getCompetencyScores(anyInt(), anyInt())).thenReturn(List.of());

        StudentAbilityDeltaLog log = new StudentAbilityDeltaLog();
        log.setAbilityPointId("ab-1");
        log.setBeforeScore(60);
        log.setAfterScore(80);
        log.setCreatedAt(LocalDateTime.now());
        when(deltaMapper.selectList(any())).thenReturn(List.of(log));

        Map<String, Object> result = service.getAbilityRadar("2024001", "101", null, null);

        String summary = (String) result.get("summary");
        assertTrue(summary.contains("逻辑思维"));
        assertTrue(summary.contains("1"));
    }

    @Test
    void getAbilityRadarUsesReasonFromDeltaLog() {
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(ability("ab-1", "逻辑思维", "")));
        when(profileService.getCompetencyScores(anyInt(), anyInt())).thenReturn(List.of());

        StudentAbilityDeltaLog log = new StudentAbilityDeltaLog();
        log.setAbilityPointId("ab-1");
        log.setBeforeScore(60);
        log.setAfterScore(80);
        log.setAiSummary("AI分析：进步明显");
        log.setReason("完成测验");
        log.setCreatedAt(LocalDateTime.now());
        when(deltaMapper.selectList(any())).thenReturn(List.of(log));

        Map<String, Object> result = service.getAbilityRadar("2024001", "101", null, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dimensions = (List<Map<String, Object>>) result.get("dimensions");
        assertEquals("AI分析：进步明显", dimensions.get(0).get("reason"));
    }

    @Test
    void getAbilityRadarUsesFallbackReasonWhenAiSummaryEmpty() {
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(ability("ab-1", "逻辑思维", "")));
        when(profileService.getCompetencyScores(anyInt(), anyInt())).thenReturn(List.of());

        StudentAbilityDeltaLog log = new StudentAbilityDeltaLog();
        log.setAbilityPointId("ab-1");
        log.setBeforeScore(60);
        log.setAfterScore(80);
        log.setAiSummary("");
        log.setReason("完成测验");
        log.setCreatedAt(LocalDateTime.now());
        when(deltaMapper.selectList(any())).thenReturn(List.of(log));

        Map<String, Object> result = service.getAbilityRadar("2024001", "101", null, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dimensions = (List<Map<String, Object>>) result.get("dimensions");
        assertEquals("完成测验", dimensions.get(0).get("reason"));
    }

    @Test
    void getAbilityRadarEmptyDimensionsWhenNoAbilities() {
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of());
        when(deltaMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = service.getAbilityRadar("2024001", "101", null, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dimensions = (List<Map<String, Object>>) result.get("dimensions");
        assertTrue(dimensions.isEmpty());
    }

    @Test
    void getAbilityRadarMultipleAbilitiesWithDifferentDeltas() {
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(
                ability("ab-1", "逻辑思维", ""),
                ability("ab-2", "编程能力", ""),
                ability("ab-3", "创新能力", "")
        ));
        when(profileService.getCompetencyScores(anyInt(), anyInt())).thenReturn(List.of());

        StudentAbilityDeltaLog log1 = new StudentAbilityDeltaLog();
        log1.setAbilityPointId("ab-1");
        log1.setBeforeScore(60);
        log1.setAfterScore(70);
        log1.setCreatedAt(LocalDateTime.now());
        StudentAbilityDeltaLog log2 = new StudentAbilityDeltaLog();
        log2.setAbilityPointId("ab-2");
        log2.setBeforeScore(50);
        log2.setAfterScore(90);
        log2.setCreatedAt(LocalDateTime.now());
        when(deltaMapper.selectList(any())).thenReturn(List.of(log1, log2));

        Map<String, Object> result = service.getAbilityRadar("2024001", "101", null, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dimensions = (List<Map<String, Object>>) result.get("dimensions");
        assertEquals(3, dimensions.size());

        // ab-2 has the biggest delta (40 vs 10), should be the topChange
        String summary = (String) result.get("summary");
        assertTrue(summary.contains("编程能力"));
    }

    @Test
    void getAbilityRadarFiltersBlankAbilityPointIdInDeltas() {
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(ability("ab-1", "逻辑思维", "")));
        when(profileService.getCompetencyScores(anyInt(), anyInt())).thenReturn(List.of());

        StudentAbilityDeltaLog blankLog = new StudentAbilityDeltaLog();
        blankLog.setAbilityPointId("");
        blankLog.setBeforeScore(10);
        blankLog.setAfterScore(20);
        blankLog.setCreatedAt(LocalDateTime.now());
        when(deltaMapper.selectList(any())).thenReturn(List.of(blankLog));

        Map<String, Object> result = service.getAbilityRadar("2024001", "101", null, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dimensions = (List<Map<String, Object>>) result.get("dimensions");
        assertEquals(50, dimensions.get(0).get("beforeScore")); // no delta applied
    }

    // ============ helpers ============

    private static AbilityPoint ability(String id, String name, String description) {
        AbilityPoint a = new AbilityPoint();
        a.setAbilityPointId(id);
        a.setName(name);
        a.setDescription(description);
        return a;
    }
}

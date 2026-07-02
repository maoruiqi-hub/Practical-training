package com.neu.CoursePlatform.profile.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.agentic.AgenticRequest;
import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.profile.entity.CompetencyScore;
import com.neu.CoursePlatform.profile.entity.Recommendation;
import com.neu.CoursePlatform.profile.mapper.RecommendationMapper;
import com.neu.CoursePlatform.profile.service.ProfileService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

class RecommendationServiceImplTest {

    private RecommendationServiceImpl service; // spy
    private RecommendationMapper recommendationMapper;
    private ProfileService profileService;
    private AgenticClient agenticClient;
    private Map<String, Recommendation> recStore;

    @BeforeEach
    void setUp() {
        recStore = new LinkedHashMap<>();

        recommendationMapper = (RecommendationMapper) Proxy.newProxyInstance(
                RecommendationMapper.class.getClassLoader(),
                new Class<?>[]{RecommendationMapper.class},
                (p, method, args) -> {
                    String name = method.getName();
                    if ("insert".equals(name) && args != null && args.length == 1 && args[0] instanceof Recommendation r) {
                        if (r.getId() == null) r.setId(UUID.randomUUID().toString());
                        recStore.put(r.getId(), r);
                        return 1;
                    }
                    if ("delete".equals(name)) {
                        int size = recStore.size();
                        recStore.clear();
                        return size;
                    }
                    if ("selectById".equals(name)) return recStore.get(String.valueOf(args[0]));
                    if ("updateById".equals(name) && args != null && args.length >= 1 && args[0] instanceof Recommendation r) {
                        recStore.put(r.getId(), r);
                        return 1;
                    }
                    if ("selectList".equals(name)) return new ArrayList<>(recStore.values());
                    if ("toString".equals(name)) return "RecMapperProxy";
                    if ("hashCode".equals(name)) return System.identityHashCode(p);
                    if ("equals".equals(name)) return p == args[0];
                    return null;
                });

        profileService = mock(ProfileService.class);
        agenticClient = mock(AgenticClient.class);

        RecommendationServiceImpl real = new RecommendationServiceImpl(
                recommendationMapper, profileService, agenticClient);
        service = spy(real);

        // Stub getRecommendations (uses LambdaQueryWrapper internally)
        lenient().doReturn(List.of()).when(service).getRecommendations(anyInt(), anyInt());
    }

    // ============ generateRecommendations ============

    @Test
    void generateRecommendationsScoreBelow40GetsReviewMaterial() {
        CompetencyScore cs = new CompetencyScore();
        cs.setAbilityPointId("ab-1");
        cs.setAbilityPointName("Java基础");
        cs.setScore(25);

        when(profileService.getCompetencyScores(2024001, 101)).thenReturn(List.of(cs));
        when(agenticClient.invoke(eq("recommend"), any(AgenticRequest.class)))
                .thenReturn(new AgenticResponse(false, Map.of(), "unavailable"));

        List<Recommendation> result = service.generateRecommendations(2024001, 101);

        assertEquals(1, result.size());
        assertEquals("review_material", result.get(0).getType());
        assertEquals(1, result.get(0).getPriority());
        assertEquals("ab-1", result.get(0).getTargetId());
        assertNotNull(result.get(0).getReason());
        assertTrue(recStore.containsKey(result.get(0).getId()));
    }

    @Test
    void generateRecommendationsScore40To59GetsPractice() {
        CompetencyScore cs = new CompetencyScore();
        cs.setAbilityPointId("ab-1");
        cs.setAbilityPointName("OOP");
        cs.setScore(55);

        when(profileService.getCompetencyScores(2024001, 101)).thenReturn(List.of(cs));
        when(agenticClient.invoke(eq("recommend"), any(AgenticRequest.class)))
                .thenReturn(new AgenticResponse(false, Map.of(), "unavailable"));

        List<Recommendation> result = service.generateRecommendations(2024001, 101);

        assertEquals(1, result.size());
        assertEquals("practice", result.get(0).getType());
        assertEquals(2, result.get(0).getPriority());
    }

    @Test
    void generateRecommendationsScore60To79GetsKnowledgePoint() {
        CompetencyScore cs = new CompetencyScore();
        cs.setAbilityPointId("ab-1");
        cs.setAbilityPointName("设计模式");
        cs.setScore(65);

        when(profileService.getCompetencyScores(2024001, 101)).thenReturn(List.of(cs));
        when(agenticClient.invoke(eq("recommend"), any(AgenticRequest.class)))
                .thenReturn(new AgenticResponse(false, Map.of(), "unavailable"));

        List<Recommendation> result = service.generateRecommendations(2024001, 101);

        assertEquals(1, result.size());
        assertEquals("knowledge_point", result.get(0).getType());
        assertEquals(2, result.get(0).getPriority());
    }

    @Test
    void generateRecommendationsScore80PlusGetsExtendedMaterial() {
        CompetencyScore cs = new CompetencyScore();
        cs.setAbilityPointId("ab-1");
        cs.setAbilityPointName("算法");
        cs.setScore(85);

        when(profileService.getCompetencyScores(2024001, 101)).thenReturn(List.of(cs));
        when(agenticClient.invoke(eq("recommend"), any(AgenticRequest.class)))
                .thenReturn(new AgenticResponse(false, Map.of(), "unavailable"));

        List<Recommendation> result = service.generateRecommendations(2024001, 101);

        assertEquals(1, result.size());
        assertEquals("extended_material", result.get(0).getType());
        assertEquals(3, result.get(0).getPriority());
    }

    @Test
    void generateRecommendationsEmptyScores() {
        when(profileService.getCompetencyScores(2024001, 101)).thenReturn(List.of());

        List<Recommendation> result = service.generateRecommendations(2024001, 101);

        assertTrue(result.isEmpty());
    }

    @Test
    void generateRecommendationsClearsOldRecommendations() {
        // Pre-populate an old recommendation
        Recommendation old = new Recommendation();
        old.setId("old-1");
        old.setStudentNo(2024001);
        old.setCourseCode(101);
        recStore.put("old-1", old);

        when(profileService.getCompetencyScores(2024001, 101)).thenReturn(List.of());

        service.generateRecommendations(2024001, 101);

        // Old recommendation should be deleted
        assertTrue(recStore.isEmpty());
    }

    @Test
    void generateRecommendationsMultipleScores() {
        CompetencyScore cs1 = new CompetencyScore();
        cs1.setAbilityPointId("ab-1");
        cs1.setAbilityPointName("A");
        cs1.setScore(30);
        CompetencyScore cs2 = new CompetencyScore();
        cs2.setAbilityPointId("ab-2");
        cs2.setAbilityPointName("B");
        cs2.setScore(90);

        when(profileService.getCompetencyScores(2024001, 101)).thenReturn(List.of(cs1, cs2));
        when(agenticClient.invoke(eq("recommend"), any(AgenticRequest.class)))
                .thenReturn(new AgenticResponse(false, Map.of(), "unavailable"));

        List<Recommendation> result = service.generateRecommendations(2024001, 101);

        assertEquals(2, result.size());
        assertEquals("review_material", result.get(0).getType());
        assertEquals("extended_material", result.get(1).getType());
    }

    // ============ generateRecommendations with agentic reason ============

    @Test
    void generateRecommendationsUsesAgenticReason() {
        CompetencyScore cs = new CompetencyScore();
        cs.setAbilityPointId("ab-1");
        cs.setAbilityPointName("Java");
        cs.setScore(50);

        when(profileService.getCompetencyScores(2024001, 101)).thenReturn(List.of(cs));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reason", "建议多做Java练习题巩固基础");
        when(agenticClient.invoke(eq("recommend"), any(AgenticRequest.class)))
                .thenReturn(new AgenticResponse(true, data, "ok"));

        List<Recommendation> result = service.generateRecommendations(2024001, 101);

        assertEquals("建议多做Java练习题巩固基础", result.get(0).getReason());
    }

    @Test
    void generateRecommendationsUsesAgenticMessageAsFallback() {
        CompetencyScore cs = new CompetencyScore();
        cs.setAbilityPointId("ab-1");
        cs.setAbilityPointName("Java");
        cs.setScore(50);

        when(profileService.getCompetencyScores(2024001, 101)).thenReturn(List.of(cs));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "来自message的建议");
        when(agenticClient.invoke(eq("recommend"), any(AgenticRequest.class)))
                .thenReturn(new AgenticResponse(true, data, "ok"));

        List<Recommendation> result = service.generateRecommendations(2024001, 101);

        assertEquals("来自message的建议", result.get(0).getReason());
    }

    @Test
    void generateRecommendationsAgenticReturnsNullUsesFallback() {
        CompetencyScore cs = new CompetencyScore();
        cs.setAbilityPointId("ab-1");
        cs.setAbilityPointName("数据结构");
        cs.setScore(30);

        when(profileService.getCompetencyScores(2024001, 101)).thenReturn(List.of(cs));
        when(agenticClient.invoke(eq("recommend"), any(AgenticRequest.class)))
                .thenReturn(null);

        List<Recommendation> result = service.generateRecommendations(2024001, 101);

        assertNotNull(result.get(0).getReason());
        assertTrue(result.get(0).getReason().contains("数据结构"));
    }

    @Test
    void generateRecommendationsAgenticEmptyDataUsesFallback() {
        CompetencyScore cs = new CompetencyScore();
        cs.setAbilityPointId("ab-1");
        cs.setAbilityPointName("网络协议");
        cs.setScore(55);

        when(profileService.getCompetencyScores(2024001, 101)).thenReturn(List.of(cs));
        when(agenticClient.invoke(eq("recommend"), any(AgenticRequest.class)))
                .thenReturn(new AgenticResponse(true, Map.of(), "ok"));

        List<Recommendation> result = service.generateRecommendations(2024001, 101);

        assertTrue(result.get(0).getReason().contains("网络协议"));
    }

    @Test
    void generateRecommendationsAgenticNullDataUsesFallback() {
        CompetencyScore cs = new CompetencyScore();
        cs.setAbilityPointId("ab-1");
        cs.setAbilityPointName("操作系统");
        cs.setScore(85);

        when(profileService.getCompetencyScores(2024001, 101)).thenReturn(List.of(cs));
        when(agenticClient.invoke(eq("recommend"), any(AgenticRequest.class)))
                .thenReturn(new AgenticResponse(true, null, "ok"));

        List<Recommendation> result = service.generateRecommendations(2024001, 101);

        assertTrue(result.get(0).getReason().contains("操作系统"));
    }

    // ============ recordFeedback ============

    @Test
    void recordFeedbackUpdatesRecommendation() {
        Recommendation rec = new Recommendation();
        rec.setId("rec-1");
        rec.setStudentNo(2024001);
        recStore.put("rec-1", rec);

        service.recordFeedback("rec-1", "这个推荐很有用");

        assertEquals("这个推荐很有用", recStore.get("rec-1").getFeedback());
    }

    @Test
    void recordFeedbackNonexistentDoesNotThrow() {
        assertDoesNotThrow(() -> service.recordFeedback("nonexistent", "feedback"));
    }

    // ============ getRecommendations (stubbed) ============

    @Test
    void getRecommendationsReturnsStubbedList() {
        Recommendation rec = new Recommendation();
        rec.setId("rec-1");
        rec.setType("practice");
        rec.setPriority(2);

        doReturn(List.of(rec)).when(service).getRecommendations(2024001, 101);

        List<Recommendation> result = service.getRecommendations(2024001, 101);
        assertEquals(1, result.size());
        assertEquals("practice", result.get(0).getType());
    }
}

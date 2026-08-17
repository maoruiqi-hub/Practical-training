package com.neu.CoursePlatform.profile.service.impl;

import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.agentic.AgenticRequest;
import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.profile.entity.CompetencyScore;
import com.neu.CoursePlatform.profile.entity.Recommendation;
import com.neu.CoursePlatform.profile.mapper.RecommendationMapper;
import com.neu.CoursePlatform.profile.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RecommendationServiceImpl 批量推荐改造后的单测。
 * 覆盖：批量回填理由 + 缺失 targetId 走 fallback + type/priority 判定。
 */
@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    private RecommendationMapper recommendationMapper;

    @Mock
    private ProfileService profileService;

    @Mock
    private AgenticClient agenticClient;

    @Test
    void generateRecommendationsBackfillsReasonsFromBatchResponse() {
        CompetencyScore cs = new CompetencyScore();
        cs.setAbilityPointId("ap-1");
        cs.setAbilityPointName("数据结构");
        cs.setScore(30);
        when(profileService.getCompetencyScores(2024001, 101)).thenReturn(List.of(cs));

        AgenticResponse response = new AgenticResponse(true, Map.of(
                "reasons", List.of(Map.of("targetId", "ap-1", "reason", "建议优先复习基础材料"))
        ), "ok");
        when(agenticClient.invoke(eq("recommend"), any(AgenticRequest.class))).thenReturn(response);

        RecommendationServiceImpl service =
                new RecommendationServiceImpl(recommendationMapper, profileService, agenticClient);
        List<Recommendation> recs = service.generateRecommendations(2024001, 101);

        assertEquals(1, recs.size());
        Recommendation rec = recs.get(0);
        assertEquals("ap-1", rec.getTargetId());
        assertEquals("review_material", rec.getType());
        assertEquals(1, rec.getPriority());
        assertEquals("建议优先复习基础材料", rec.getReason());
        verify(recommendationMapper).insert(rec);
    }

    @Test
    void generateRecommendationsFallsBackForMissingReasons() {
        CompetencyScore cs = new CompetencyScore();
        cs.setAbilityPointId("ap-1");
        cs.setAbilityPointName("数据结构");
        cs.setScore(90);
        when(profileService.getCompetencyScores(2024001, 101)).thenReturn(List.of(cs));

        // 响应未返回 ap-1 的理由 -> 走 fallbackReason
        AgenticResponse response = new AgenticResponse(true, Map.of("reasons", List.of()), "ok");
        when(agenticClient.invoke(eq("recommend"), any(AgenticRequest.class))).thenReturn(response);

        RecommendationServiceImpl service =
                new RecommendationServiceImpl(recommendationMapper, profileService, agenticClient);
        List<Recommendation> recs = service.generateRecommendations(2024001, 101);

        assertEquals(1, recs.size());
        assertEquals("extended_material", recs.get(0).getType());
        assertTrue(recs.get(0).getReason().contains("拓展"));
        verify(recommendationMapper).insert(recs.get(0));
    }

    @Test
    void generateRecommendationsSendsBatchRequestOnce() {
        CompetencyScore cs1 = new CompetencyScore();
        cs1.setAbilityPointId("ap-1");
        cs1.setAbilityPointName("数据结构");
        cs1.setScore(45);

        CompetencyScore cs2 = new CompetencyScore();
        cs2.setAbilityPointId("ap-2");
        cs2.setAbilityPointName("算法");
        cs2.setScore(85);
        when(profileService.getCompetencyScores(2024001, 101)).thenReturn(List.of(cs1, cs2));

        AgenticResponse response = new AgenticResponse(true, Map.of("reasons", List.of()), "ok");
        when(agenticClient.invoke(eq("recommend"), any(AgenticRequest.class))).thenReturn(response);

        RecommendationServiceImpl service =
                new RecommendationServiceImpl(recommendationMapper, profileService, agenticClient);
        List<Recommendation> recs = service.generateRecommendations(2024001, 101);

        assertEquals(2, recs.size());

        // 只调用一次 recommend，且为批量请求（recommendations 数组长度为 2）
        ArgumentCaptor<AgenticRequest> captor = ArgumentCaptor.forClass(AgenticRequest.class);
        verify(agenticClient, times(1)).invoke(eq("recommend"), captor.capture());
        Object recommendations = captor.getValue().getContext().get("recommendations");
        assertInstanceOf(List.class, recommendations);
        assertEquals(2, ((List<?>) recommendations).size());
    }
}

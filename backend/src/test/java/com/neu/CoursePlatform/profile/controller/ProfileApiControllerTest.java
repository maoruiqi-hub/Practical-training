package com.neu.CoursePlatform.profile.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.profile.entity.Achievement;
import com.neu.CoursePlatform.profile.entity.CompetencyScore;
import com.neu.CoursePlatform.profile.entity.Recommendation;
import com.neu.CoursePlatform.profile.service.IncentiveService;
import com.neu.CoursePlatform.profile.service.ProfileService;
import com.neu.CoursePlatform.profile.service.RecommendationService;
import com.neu.CoursePlatform.service.AbilityRadarService;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import com.neu.CoursePlatform.service.TowerQuestionPackService;
import com.neu.CoursePlatform.service.TowerRunService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;

class ProfileApiControllerTest {

    private ProfileService profileService;
    private RecommendationService recommendationService;
    private IncentiveService incentiveService;
    private CourseGameConfigService gameConfigService;
    private TowerRunService towerRunService;
    private TowerQuestionPackService towerQuestionPackService;
    private AbilityRadarService abilityRadarService;
    private ApplicationEventPublisher applicationEventPublisher;
    private Auth auth;
    private HttpSession session;
    private ProfileApiController controller;

    @BeforeEach
    void setUp() {
        profileService = mock(ProfileService.class);
        recommendationService = mock(RecommendationService.class);
        incentiveService = mock(IncentiveService.class);
        gameConfigService = mock(CourseGameConfigService.class);
        towerRunService = mock(TowerRunService.class);
        towerQuestionPackService = mock(TowerQuestionPackService.class);
        abilityRadarService = mock(AbilityRadarService.class);
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        auth = mock(Auth.class);
        session = mock(HttpSession.class);
        controller = new ProfileApiController(profileService, recommendationService, incentiveService,
                gameConfigService, towerRunService, towerQuestionPackService, abilityRadarService,
                applicationEventPublisher, auth);

        when(auth.isLoggedIn(session)).thenReturn(true);
        when(profileService.getProfileSummary(1001, 101)).thenReturn(Map.of("studentNo", 1001));
    }

    @Test
    void profileRejectsUnauthorizedAndInvalidParameters() {
        when(auth.isLoggedIn(session)).thenReturn(false);
        Result<Map<String, Object>> unauthorized = controller.profile("1001", "101", null, session);
        assertEquals(500, unauthorized.getCode());

        when(auth.isLoggedIn(session)).thenReturn(true);
        Result<Map<String, Object>> invalid = controller.profile("abc", "101", null, session);
        assertEquals(500, invalid.getCode());

        Result<Map<String, Object>> missingCourse = controller.profile("1001", null, null, session);
        assertEquals(500, missingCourse.getCode());
    }

    @Test
    void profileAndCompetencyEndpointsDelegateToProfileService() {
        List<CompetencyScore> scores = List.of(new CompetencyScore());
        when(profileService.generateProfile(1001, 101)).thenReturn(Map.of("generated", true));
        when(profileService.getCompetencyScores(1001, 101)).thenReturn(scores);
        when(profileService.updateAllCompetencyScores(1001, 101)).thenReturn(scores);

        assertEquals(200, controller.profile("1001", "101", null, session).getCode());
        assertEquals(true, controller.generateProfile("1001", null, "101", session).getData().get("generated"));
        assertSame(scores, controller.competency("1001", "101", null, session).getData());
        assertSame(scores, controller.updateCompetency("1001", null, "101", session).getData());

        verify(profileService).getProfileSummary(1001, 101);
        verify(profileService).generateProfile(1001, 101);
        verify(profileService).getCompetencyScores(1001, 101);
        verify(profileService).updateAllCompetencyScores(1001, 101);
    }

    @Test
    void recommendationsReuseExistingOrGenerateWhenEmpty() {
        Recommendation existing = new Recommendation();
        Recommendation generated = new Recommendation();
        when(recommendationService.getRecommendations(1001, 101))
                .thenReturn(List.of(existing))
                .thenReturn(List.of());
        when(recommendationService.generateRecommendations(1001, 101)).thenReturn(List.of(generated));

        assertSame(existing, controller.recommendations("1001", "101", null, session).getData().get(0));
        assertSame(generated, controller.recommendations("1001", "101", null, session).getData().get(0));
        assertSame(generated, controller.generateRecommendations("1001", null, "101", session).getData().get(0));
    }

    @Test
    void towerMapUsesRunNodesAndFallsBackToProfileMap() {
        List<Map<String, Object>> nodes = List.of(Map.of("nodeId", "n1"));
        when(towerRunService.getOrCreateActiveRun("1001", "101")).thenReturn(Map.of("nodes", nodes));

        assertSame(nodes, controller.towerMap("1001", "101", null, session).getData());

        List<Map<String, Object>> fallback = List.of(Map.of("knowledgePointId", "kp1"));
        when(towerRunService.getOrCreateActiveRun("1001", "101")).thenReturn(Map.of("nodes", "bad"));
        when(profileService.getTowerMap(1001, 101)).thenReturn(fallback);

        assertSame(fallback, controller.towerMap("1001", "101", null, session).getData());
    }

    @Test
    void towerRunEndpointsReturnServiceDataAndConvertRuntimeErrors() {
        when(towerRunService.getOrCreateActiveRun("1001", "101")).thenReturn(Map.of("runId", "r1"));
        when(towerRunService.generateRun("1001", "101", true)).thenReturn(Map.of("forced", true));
        when(towerRunService.getNode("1001", "r1", "n1")).thenReturn(Map.of("nodeId", "n1"));
        when(towerRunService.enterNode("1001", "r1", "n1")).thenThrow(new IllegalStateException("locked"));
        when(towerRunService.completeNode(eq("1001"), eq("r1"), eq("n1"), anyMap())).thenReturn(Map.of("cleared", true));
        when(towerRunService.diagnoseNode(eq("1001"), eq("r1"), eq("n1"), anyMap())).thenReturn(Map.of("status", "perfect"));

        assertEquals("r1", controller.towerRun("1001", "101", null, session).getData().get("runId"));
        assertEquals(true, controller.generateTowerRun("1001", "101", null, true, session).getData().get("forced"));
        assertEquals("n1", controller.towerNode("1001", "r1", "n1", session).getData().get("nodeId"));
        assertEquals(500, controller.enterTowerNode("1001", "r1", "n1", session).getCode());
        assertEquals(true, controller.completeTowerNode("1001", "r1", "n1", Map.of("result", "cleared"), session)
                .getData().get("cleared"));
        assertEquals("perfect", controller.diagnoseTowerNode("1001", "r1", "n1", Map.of(), session)
                .getData().get("status"));
    }

    @Test
    void towerQuestionPackAndAbilityEndpointsDelegate() {
        when(towerQuestionPackService.getOrCreateQuestionPack("1001", "r1", "n1", "battle"))
                .thenReturn(Map.of("packId", "p1"));
        when(towerQuestionPackService.regenerateQuestionPack("1001", "r1", "n1", "elite"))
                .thenReturn(Map.of("packId", "p2"));
        when(towerRunService.getAbilityDeltas("1001", "101", "r1"))
                .thenReturn(List.of(Map.of("delta", 3)));
        when(abilityRadarService.getAbilityRadar("1001", "101", "r1", "n1"))
                .thenReturn(Map.of("radar", true));

        assertEquals("p1", controller.towerQuestionPack("1001", "r1", "n1", "battle", session).getData().get("packId"));
        assertEquals("p2", controller.regenerateTowerQuestionPack("1001", "r1", "n1", "elite", session).getData().get("packId"));
        assertEquals(3, controller.abilityDeltas("1001", "101", null, "r1", session).getData().get(0).get("delta"));
        assertEquals(true, controller.abilityRadar("1001", null, "101", "r1", "n1", session).getData().get("radar"));
    }

    @Test
    void growthAchievementAndLeaderboardEndpointsMapInputs() {
        Achievement achievement = new Achievement();
        when(incentiveService.checkAndAwardBadges(eq(1001), eq(101), anyInt(), anyInt(),
                anyBoolean(), anyBoolean(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(achievement));
        when(incentiveService.getLeaderboard(101, "exp")).thenReturn(List.of(Map.of("studentNo", 1001)));

        Result<Void> growth = controller.addGrowth("1001", "101", null,
                Map.of("amount", 8, "source", "task", "source_id", "t1"), session);
        assertEquals(200, growth.getCode());
        verify(profileService).addGrowth(1001, 101, 8, "task", "t1");

        Result<List<Achievement>> awarded = controller.awardAchievements("1001", "101", null,
                Map.of("totalCorrect", 10, "consecutiveCorrect", 3, "timedComplete", true, "fullScore", true),
                session);
        assertSame(achievement, awarded.getData().get(0));

        Result<List<Map<String, Object>>> leaderboard = controller.leaderboard(null, "101", "exp", session);
        assertEquals(1001, leaderboard.getData().get(0).get("studentNo"));
    }

    @Test
    void receiveGameEventValidatesPublishesAndHonorsDisabledConfig() {
        when(gameConfigService.isEnabled("101")).thenReturn(false, true);

        Result<Map<String, Object>> missing = controller.receiveGameEvent("1001", Map.of("course_id", "101"));
        assertEquals(500, missing.getCode());

        Result<Map<String, Object>> disabled = controller.receiveGameEvent("1001",
                Map.of("course_id", "101", "event_type", "answer_correct"));
        assertEquals(200, disabled.getCode());
        verify(applicationEventPublisher, never()).publishEvent(any());

        Result<Map<String, Object>> enabled = controller.receiveGameEvent("1001",
                Map.of("courseCode", "101", "eventType", "boss_defeated", "source_id", "node-1"));
        assertEquals(200, enabled.getCode());
        ArgumentCaptor<GameEvent> eventCaptor = ArgumentCaptor.forClass(GameEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("boss_defeated", eventCaptor.getValue().getEventType());
        assertEquals("node-1", eventCaptor.getValue().getSourceId());
    }
}

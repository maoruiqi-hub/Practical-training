package com.neu.CoursePlatform.profile.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.profile.entity.Achievement;
import com.neu.CoursePlatform.profile.entity.CompetencyScore;
import com.neu.CoursePlatform.profile.entity.Recommendation;
import com.neu.CoursePlatform.profile.service.IncentiveService;
import com.neu.CoursePlatform.profile.service.ProfileService;
import com.neu.CoursePlatform.profile.service.RecommendationService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class ProfileControllerTest {

    private ProfileService profileService;
    private RecommendationService recommendationService;
    private IncentiveService incentiveService;
    private Auth auth;
    private HttpSession session;
    private ProfileController controller;

    @BeforeEach
    void setUp() {
        profileService = mock(ProfileService.class);
        recommendationService = mock(RecommendationService.class);
        incentiveService = mock(IncentiveService.class);
        auth = mock(Auth.class);
        session = mock(HttpSession.class);
        controller = new ProfileController(profileService, recommendationService, incentiveService, auth);

        when(auth.isLoggedIn(session)).thenReturn(true);
        when(auth.canModifyCourse(session, "101")).thenReturn(true);
        when(profileService.getProfileSummary(1001, 101)).thenReturn(Map.of("studentNo", 1001));
    }

    @Test
    void teacherCourseStudentsChecksLoginAndCoursePermission() {
        when(auth.isLoggedIn(session)).thenReturn(false);
        assertEquals(500, controller.courseStudents(101, session).getCode());

        when(auth.isLoggedIn(session)).thenReturn(true);
        when(auth.canModifyCourse(session, "101")).thenReturn(false);
        assertEquals(500, controller.courseStudents(101, session).getCode());

        when(auth.canModifyCourse(session, "101")).thenReturn(true);
        when(profileService.listCourseStudentProfiles(101)).thenReturn(List.of(Map.of("studentNo", 1001)));
        assertEquals(1001, controller.courseStudents(101, session).getData().get(0).get("studentNo"));
    }

    @Test
    void studentAndTeacherCanReadProfileViews() {
        Student student = new Student();
        student.setStudentNo("1001");
        when(session.getAttribute("student")).thenReturn(student);
        when(profileService.getTowerMap(1001, 101)).thenReturn(List.of(Map.of("floor", 1)));
        List<CompetencyScore> scores = List.of(new CompetencyScore());
        when(profileService.getCompetencyScores(1001, 101)).thenReturn(scores);

        assertEquals(1001, controller.summary(1001, 101, session).getData().get("studentNo"));
        assertEquals(1, controller.towerMap(1001, 101, session).getData().get(0).get("floor"));
        assertSame(scores, controller.competency(1001, 101, session).getData());

        when(session.getAttribute("student")).thenReturn(null);
        when(auth.canModifyCourse(session, "101")).thenReturn(false);
        assertEquals(500, controller.summary(1001, 101, session).getCode());
    }

    @Test
    void recommendationsFeedbackAchievementsAndLeaderboardDelegate() {
        Student student = new Student();
        student.setStudentNo("1001");
        when(session.getAttribute("student")).thenReturn(student);
        Recommendation generated = new Recommendation();
        Achievement achievement = new Achievement();
        when(recommendationService.getRecommendations(1001, 101)).thenReturn(List.of()).thenReturn(List.of(generated));
        when(recommendationService.generateRecommendations(1001, 101)).thenReturn(List.of(generated));
        when(incentiveService.getAchievements(1001, 101)).thenReturn(List.of(achievement));
        when(incentiveService.getTitle(1001, 101)).thenReturn("勇者");
        when(incentiveService.getLeaderboard(101, "exp")).thenReturn(List.of(Map.of("studentNo", 1001)));

        assertSame(generated, controller.recommendations(1001, 101, session).getData().get(0));
        assertSame(generated, controller.generateRecommendations(1001, 101, session).getData().get(0));
        assertEquals(200, controller.feedback("rec-1", "helpful", session).getCode());
        verify(recommendationService).recordFeedback("rec-1", "helpful");
        assertSame(achievement, controller.achievements(1001, 101, session).getData().get(0));
        assertEquals("勇者", controller.title(1001, 101, session).getData());
        assertEquals(1001, controller.leaderboard(101, "exp", session).getData().get(0).get("studentNo"));
    }

    @Test
    void writeEndpointsMapRequestBodies() {
        when(profileService.generateProfile(1001, 101)).thenReturn(Map.of("generated", true));
        when(profileService.updateAllCompetencyScores(1001, 101)).thenReturn(List.of(new CompetencyScore()));
        when(profileService.getCompetencyHistory(1001, 101, "ab1")).thenReturn(List.of(Map.of("score", 80)));
        when(profileService.getGrowthHistory(1001, 101)).thenReturn(List.of(Map.of("amount", 10)));
        when(profileService.generateTestFeedback(1001, 101)).thenReturn(Map.of("summary", "ok"));
        when(incentiveService.checkAndAwardBadges(eq(1001), eq(101), anyInt(), anyInt(),
                anyBoolean(), anyBoolean(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(new Achievement()));

        assertEquals(200, controller.submit(1001, 101, true, "quiz", "ab1", session).getCode());
        verify(profileService).updateProfileFromSubmission(1001, 101, true, "quiz");
        verify(profileService).updateCompetencyScores(1001, 101, "ab1", true);

        assertEquals(200, controller.addGrowth(1001, 101, Map.of("amount", 5, "source", "task", "sourceId", "t1"), session).getCode());
        verify(profileService).addGrowth(1001, 101, 5, "task", "t1");

        assertEquals(1, controller.awardAchievements(1001, 101,
                Map.of("totalCorrect", 10, "consecutiveCorrect", 3, "timedComplete", true, "fullScore", true),
                session).getData().size());
        assertEquals(true, controller.generateProfile(1001, 101, session).getData().get("generated"));
        assertEquals(1, controller.updateCompetency(1001, 101, session).getData().size());
        assertEquals(80, controller.competencyHistory(1001, 101, "ab1", session).getData().get(0).get("score"));
        assertEquals(10, controller.growthHistory(1001, 101, session).getData().get(0).get("amount"));
        assertEquals("ok", controller.testFeedback(1001, 101, session).getData().get("summary"));
    }

    @Test
    void receiveGameEventHandlesValidationAndSupportedEvents() {
        assertEquals(500, controller.receiveGameEvent(Map.of("studentNo", "1001", "courseCode", "101")).getCode());

        controller.receiveGameEvent(Map.of("studentNo", "1001", "courseCode", "101",
                "eventType", GameEventTypes.ANSWER_CORRECT, "taskType", "quiz", "abilityPointId", "ab1"));
        verify(profileService).updateProfileFromSubmission(1001, 101, true, "quiz");
        verify(profileService).updateCompetencyScores(1001, 101, "ab1", true);

        controller.receiveGameEvent(Map.of("studentNo", "1001", "courseCode", "101",
                "eventType", GameEventTypes.ANSWER_WRONG, "taskType", "quiz"));
        verify(profileService).updateProfileFromSubmission(1001, 101, false, "quiz");

        controller.receiveGameEvent(Map.of("studentNo", "1001", "courseCode", "101",
                "eventType", GameEventTypes.FLOOR_CLEARED, "floor", "kp1"));
        controller.receiveGameEvent(Map.of("studentNo", "1001", "courseCode", "101",
                "eventType", GameEventTypes.BOSS_DEFEATED));
        controller.receiveGameEvent(Map.of("studentNo", "1001", "courseCode", "101",
                "eventType", GameEventTypes.SUPPLY_USED, "supplyType", "hint"));
        controller.receiveGameEvent(Map.of("studentNo", "1001", "courseCode", "101",
                "eventType", "custom"));

        verify(profileService).addGrowth(1001, 101, 80, GameEventTypes.FLOOR_CLEARED, "kp1");
        verify(profileService).addGrowth(1001, 101, 250, GameEventTypes.BOSS_DEFEATED, "");
        verify(profileService).addGrowth(1001, 101, -10, GameEventTypes.SUPPLY_USED, "hint");
        verify(profileService).addGrowth(1001, 101, 5, "custom", "");
    }

    @Test
    void stringCompatibilityEndpointsDelegate() {
        when(profileService.getProfileSummaryStr("1001", "101")).thenReturn(Map.of("studentNo", "1001"));

        assertEquals("1001", controller.summaryStr("1001", "101").getData().get("studentNo"));
        assertEquals(200, controller.addGrowthStr("1001", "101", Map.of("amount", 7, "source", "manual", "sourceId", "m1")).getCode());
        verify(profileService).addGrowthStr("1001", "101", 7, "manual", "m1");
    }
}

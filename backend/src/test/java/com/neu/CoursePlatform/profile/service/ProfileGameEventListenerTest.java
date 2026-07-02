package com.neu.CoursePlatform.profile.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.service.CourseGameConfigService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ProfileGameEventListenerTest {

    private ProfileGameEventListener listener;
    private ProfileService profileService;
    private CourseGameConfigService gameConfigService;

    @BeforeEach
    void setUp() {
        profileService = mock(ProfileService.class);
        gameConfigService = mock(CourseGameConfigService.class);
        listener = new ProfileGameEventListener(profileService, gameConfigService);
        lenient().when(gameConfigService.isEnabled(anyString())).thenReturn(true);
    }

    // ============ null/disabled guard ============

    @Test
    void onGameEventNoOpWhenEventNull() {
        listener.onGameEvent(null);
        verifyNoInteractions(profileService);
    }

    @Test
    void onGameEventNoOpWhenStudentIdNull() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.FLOOR_CLEARED)
                .courseId("101").build();
        listener.onGameEvent(event);
        verifyNoInteractions(profileService);
    }

    @Test
    void onGameEventNoOpWhenCourseIdNull() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.FLOOR_CLEARED)
                .studentId("2024001").build();
        listener.onGameEvent(event);
        verifyNoInteractions(profileService);
    }

    @Test
    void onGameEventNoOpWhenGameDisabled() {
        when(gameConfigService.isEnabled("101")).thenReturn(false);
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.FLOOR_CLEARED)
                .studentId("2024001").courseId("101").build();

        listener.onGameEvent(event);
        verifyNoInteractions(profileService);
    }

    @Test
    void onGameEventNoOpWhenStudentIdNotParseable() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.FLOOR_CLEARED)
                .studentId("abc").courseId("101").build();

        listener.onGameEvent(event);
        verifyNoInteractions(profileService);
    }

    // ============ ANSWER_CORRECT ============

    @Test
    void answerCorrectUpdatesProfileAndCompetency() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.ANSWER_CORRECT)
                .studentId("2024001").courseId("101")
                .sourceId("src-1")
                .payload(Map.of("ability_point_id", "ab-1"))
                .build();

        listener.onGameEvent(event);

        verify(profileService).updateProfileFromSubmission(eq(2024001), eq(101), eq(true), anyString());
        verify(profileService).updateCompetencyScores(2024001, 101, "ab-1", true);
    }

    // ============ ANSWER_WRONG ============

    @Test
    void answerWrongUpdatesProfileAndCompetency() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.ANSWER_WRONG)
                .studentId("2024001").courseId("101")
                .sourceId("src-1")
                .payload(Map.of("knowledgePointId", "kp-1"))
                .build();

        listener.onGameEvent(event);

        verify(profileService).updateProfileFromSubmission(eq(2024001), eq(101), eq(false), anyString());
        verify(profileService).updateCompetencyScores(2024001, 101, "kp-1", false);
    }

    // ============ ANSWER_SKIPPED ============

    @Test
    void answerSkippedAppliesDelta() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.ANSWER_SKIPPED)
                .studentId("2024001").courseId("101").sourceId("src-1")
                .build();

        listener.onGameEvent(event);

        verify(profileService).applyGameDelta(2024001, 101, 0, 0, 0, 0, 0, -1,
                GameEventTypes.ANSWER_SKIPPED, "src-1");
    }

    // ============ FLOOR_CLEARED ============

    @Test
    void floorClearedAppliesDeltaAndCompetency() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.FLOOR_CLEARED)
                .studentId("2024001").courseId("101").sourceId("floor-1")
                .payload(Map.of("ability_point_id", "ab-1"))
                .build();

        listener.onGameEvent(event);

        verify(profileService).applyGameDelta(2024001, 101, 0, 1, 1, 80, 20, 0,
                GameEventTypes.FLOOR_CLEARED, "floor-1");
        verify(profileService).updateCompetencyScores(2024001, 101, "ab-1", true);
    }

    // ============ BOSS_DEFEATED ============

    @Test
    void bossDefeatedAppliesDeltaAndCompetency() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.BOSS_DEFEATED)
                .studentId("2024001").courseId("101").sourceId("boss-1")
                .payload(Map.of("ability_point_id", "ab-1"))
                .build();

        listener.onGameEvent(event);

        verify(profileService).applyGameDelta(2024001, 101, 0, 3, 3, 250, 80, 2,
                GameEventTypes.BOSS_DEFEATED, "boss-1");
        verify(profileService).updateCompetencyScores(2024001, 101, "ab-1", true);
    }

    // ============ ELITE_DEFEATED ============

    @Test
    void eliteDefeatedAppliesDeltaAndCompetency() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.ELITE_DEFEATED)
                .studentId("2024001").courseId("101").sourceId("elite-1")
                .payload(Map.of("ability_point_id", "ab-1"))
                .build();

        listener.onGameEvent(event);

        verify(profileService).applyGameDelta(2024001, 101, 0, 2, 1, 120, 40, 1,
                GameEventTypes.ELITE_DEFEATED, "elite-1");
        verify(profileService).updateCompetencyScores(2024001, 101, "ab-1", true);
    }

    // ============ FLOOR_FAILED ============

    @Test
    void floorFailedAppliesPenaltyAndCompetency() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.FLOOR_FAILED)
                .studentId("2024001").courseId("101").sourceId("floor-1")
                .payload(Map.of("ability_point_id", "ab-1"))
                .build();

        listener.onGameEvent(event);

        verify(profileService).applyGameDelta(2024001, 101, -5, 0, 0, 10, 0, -1,
                GameEventTypes.FLOOR_FAILED, "floor-1");
        verify(profileService).updateCompetencyScores(2024001, 101, "ab-1", false);
    }

    // ============ SUPPLY_USED ============

    @Test
    void supplyUsedAppliesDelta() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.SUPPLY_USED)
                .studentId("2024001").courseId("101").sourceId("potion")
                .payload(Map.of("supply_type", "potion"))
                .build();

        listener.onGameEvent(event);

        verify(profileService).applyGameDelta(2024001, 101, 30, 0, 0, 0, -10, -1,
                GameEventTypes.SUPPLY_USED, "potion");
    }

    // ============ HINT_USED ============

    @Test
    void hintUsedAppliesDelta() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.HINT_USED)
                .studentId("2024001").courseId("101").sourceId("q-1")
                .payload(Map.of("question_id", "q-1"))
                .build();

        listener.onGameEvent(event);

        verify(profileService).applyGameDelta(2024001, 101, 0, 0, 0, 0, -5, 0,
                GameEventTypes.HINT_USED, "q-1");
    }

    // ============ TREASURE_OPENED ============

    @Test
    void treasureOpenedAppliesDelta() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.TREASURE_OPENED)
                .studentId("2024001").courseId("101").sourceId("chest-1")
                .build();

        listener.onGameEvent(event);

        verify(profileService).applyGameDelta(2024001, 101, 0, 0, 0, 10, 15, 0,
                GameEventTypes.TREASURE_OPENED, "chest-1");
    }

    // ============ REST_TAKEN ============

    @Test
    void restTakenAppliesDelta() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.REST_TAKEN)
                .studentId("2024001").courseId("101").sourceId("inn")
                .build();

        listener.onGameEvent(event);

        verify(profileService).applyGameDelta(2024001, 101, 20, 0, 1, 0, 0, 1,
                GameEventTypes.REST_TAKEN, "inn");
    }

    // ============ REWARD_PICKED ============

    @Test
    void rewardPickedWithExplicitDeltas() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.REWARD_PICKED)
                .studentId("2024001").courseId("101").sourceId("reward-1")
                .payload(Map.of("hp_delta", 10, "atk_delta", 2, "coin_delta", 50))
                .build();

        listener.onGameEvent(event);

        verify(profileService).applyGameDelta(2024001, 101, 10, 2, 0, 0, 50, 0,
                GameEventTypes.REWARD_PICKED, "reward-1");
    }

    @Test
    void rewardPickedWithTypeCoin() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.REWARD_PICKED)
                .studentId("2024001").courseId("101").sourceId("reward-1")
                .payload(Map.of("reward_type", "coin"))
                .build();

        listener.onGameEvent(event);

        verify(profileService).applyGameDelta(2024001, 101, 0, 0, 0, 0, 20, 0,
                GameEventTypes.REWARD_PICKED, "reward-1");
    }

    @Test
    void rewardPickedWithTypeHeal() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.REWARD_PICKED)
                .studentId("2024001").courseId("101").sourceId("reward-1")
                .payload(Map.of("reward_type", "heal"))
                .build();

        listener.onGameEvent(event);

        verify(profileService).applyGameDelta(2024001, 101, 15, 0, 0, 0, 0, 0,
                GameEventTypes.REWARD_PICKED, "reward-1");
    }

    @Test
    void rewardPickedWithTypeRelic() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.REWARD_PICKED)
                .studentId("2024001").courseId("101").sourceId("reward-1")
                .payload(Map.of("type", "relic"))
                .build();

        listener.onGameEvent(event);

        verify(profileService).applyGameDelta(2024001, 101, 0, 0, 0, 0, 0, 1,
                GameEventTypes.REWARD_PICKED, "reward-1");
    }

    // ============ SHOP_PURCHASED ============

    @Test
    void shopPurchasedCleanse() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.SHOP_PURCHASED)
                .studentId("2024001").courseId("101").sourceId("cleanse")
                .payload(Map.of("reward_name", "cleanse_potion"))
                .build();

        listener.onGameEvent(event);

        verify(profileService).applyGameDelta(2024001, 101, 0, 0, 2, 15, -8, 0,
                GameEventTypes.SHOP_PURCHASED, "cleanse");
    }

    @Test
    void shopPurchasedNormal() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.SHOP_PURCHASED)
                .studentId("2024001").courseId("101").sourceId("sword")
                .payload(Map.of("reward_name", "sword"))
                .build();

        listener.onGameEvent(event);

        verify(profileService).applyGameDelta(2024001, 101, 0, 1, 0, 0, -10, 0,
                GameEventTypes.SHOP_PURCHASED, "sword");
    }

    // ============ EVENT_RESOLVED ============

    @Test
    void eventResolvedAppliesDelta() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.EVENT_RESOLVED)
                .studentId("2024001").courseId("101").sourceId("event-1")
                .build();

        listener.onGameEvent(event);

        verify(profileService).applyGameDelta(2024001, 101, 0, 1, 0, 20, 5, -1,
                GameEventTypes.EVENT_RESOLVED, "event-1");
    }

    // ============ unknown event type ============

    @Test
    void onGameEventUnknownTypeNoOp() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType("UNKNOWN")
                .studentId("2024001").courseId("101").build();

        listener.onGameEvent(event);
        verifyNoInteractions(profileService);
    }
}

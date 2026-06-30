package com.neu.CoursePlatform.profile.service;

import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 模块4游戏引擎入口：消费各模块发布的进程内爬塔事件。 */
@Component
public class ProfileGameEventListener {

    private final ProfileService profileService;
    private final CourseGameConfigService gameConfigService;

    public ProfileGameEventListener(ProfileService profileService, CourseGameConfigService gameConfigService) {
        this.profileService = profileService;
        this.gameConfigService = gameConfigService;
    }

    @EventListener
    public void onGameEvent(GameEvent event) {
        if (event == null || event.getStudentId() == null || event.getCourseId() == null
                || event.getEventType() == null || !gameConfigService.isEnabled(event.getCourseId())) {
            return;
        }
        Integer studentNo = parseInt(event.getStudentId());
        Integer courseCode = parseInt(event.getCourseId());
        if (studentNo == null || courseCode == null) return;

        Map<String, Object> payload = event.getPayload() == null ? Map.of() : event.getPayload();
        String sourceId = event.getSourceId() == null ? "" : event.getSourceId();
        switch (event.getEventType()) {
            case GameEventTypes.ANSWER_CORRECT -> {
                profileService.updateProfileFromSubmission(
                        studentNo, courseCode, true, stringValue(payload, "task_type", "taskType", "quiz"));
                updateCompetency(studentNo, courseCode, payload, true);
            }
            case GameEventTypes.ANSWER_WRONG -> {
                profileService.updateProfileFromSubmission(
                        studentNo, courseCode, false, stringValue(payload, "task_type", "taskType", "quiz"));
                updateCompetency(studentNo, courseCode, payload, false);
            }
            case GameEventTypes.ANSWER_SKIPPED -> profileService.applyGameDelta(studentNo, courseCode,
                    0, 0, 0, 0, 0, -1, GameEventTypes.ANSWER_SKIPPED, sourceId);
            case GameEventTypes.FLOOR_CLEARED -> profileService.applyGameDelta(studentNo, courseCode,
                    0, 1, 1, 80, 20, 0, GameEventTypes.FLOOR_CLEARED, sourceId);
            case GameEventTypes.BOSS_DEFEATED -> profileService.applyGameDelta(studentNo, courseCode,
                    0, 3, 3, 250, 80, 2, GameEventTypes.BOSS_DEFEATED, sourceId);
            case GameEventTypes.ELITE_DEFEATED -> profileService.applyGameDelta(studentNo, courseCode,
                    0, 2, 1, 120, 40, 1, GameEventTypes.ELITE_DEFEATED, sourceId);
            case GameEventTypes.FLOOR_FAILED -> profileService.applyGameDelta(studentNo, courseCode,
                    -5, 0, 0, 10, 0, -1, GameEventTypes.FLOOR_FAILED, sourceId);
            case GameEventTypes.SUPPLY_USED -> profileService.applyGameDelta(studentNo, courseCode,
                    30, 0, 0, 0, -10, -1, GameEventTypes.SUPPLY_USED,
                    stringValue(payload, "supply_type", "supplyType", sourceId));
            case GameEventTypes.HINT_USED -> profileService.applyGameDelta(studentNo, courseCode,
                    0, 0, 0, 0, -5, 0, GameEventTypes.HINT_USED,
                    stringValue(payload, "question_id", "questionId", sourceId));
            case GameEventTypes.REWARD_PICKED -> applyReward(studentNo, courseCode, payload, sourceId);
            case GameEventTypes.TREASURE_OPENED -> profileService.applyGameDelta(studentNo, courseCode,
                    0, 0, 0, 10, 15, 0, GameEventTypes.TREASURE_OPENED, sourceId);
            case GameEventTypes.REST_TAKEN -> profileService.applyGameDelta(studentNo, courseCode,
                    20, 0, 1, 0, 0, 1, GameEventTypes.REST_TAKEN, sourceId);
            case GameEventTypes.SHOP_PURCHASED -> applyShopPurchase(studentNo, courseCode, payload, sourceId);
            case GameEventTypes.EVENT_RESOLVED -> profileService.applyGameDelta(studentNo, courseCode,
                    0, 1, 0, 20, 5, -1, GameEventTypes.EVENT_RESOLVED, sourceId);
            default -> { }
        }
    }

    private void updateCompetency(Integer studentNo, Integer courseCode, Map<String, Object> payload, boolean correct) {
        String abilityPointId = firstString(payload, "ability_point_id", "abilityPointId",
                "knowledge_point_id", "knowledgePointId");
        if (abilityPointId != null) {
            profileService.updateCompetencyScores(studentNo, courseCode, abilityPointId, correct);
        }
    }

    private void applyReward(Integer studentNo, Integer courseCode, Map<String, Object> payload, String sourceId) {
        int hpDelta = intValue(payload, "hp_delta", "hpDelta", "hp");
        int atkDelta = intValue(payload, "atk_delta", "atkDelta", "atk");
        int defDelta = intValue(payload, "def_delta", "defDelta", "def");
        int expDelta = intValue(payload, "exp_delta", "expDelta", "exp");
        int coinDelta = intValue(payload, "coin_delta", "coinDelta", "coins", "coin");
        int energyDelta = intValue(payload, "energy_delta", "energyDelta", "energy");

        if (hpDelta == 0 && atkDelta == 0 && defDelta == 0 && expDelta == 0 && coinDelta == 0 && energyDelta == 0) {
            String rewardType = firstString(payload, "reward_type", "rewardType", "type");
            if ("coin".equals(rewardType)) coinDelta = 20;
            else if ("heal".equals(rewardType)) hpDelta = 15;
            else if ("relic".equals(rewardType)) energyDelta = 1;
            else atkDelta = 1;
        }

        profileService.applyGameDelta(studentNo, courseCode, hpDelta, atkDelta, defDelta,
                expDelta, coinDelta, energyDelta, GameEventTypes.REWARD_PICKED, sourceId);
    }

    private void applyShopPurchase(Integer studentNo, Integer courseCode, Map<String, Object> payload, String sourceId) {
        String rewardName = firstString(payload, "reward_name", "rewardName");
        boolean cleanse = rewardName != null && rewardName.toLowerCase().contains("clean");
        profileService.applyGameDelta(studentNo, courseCode,
                0, cleanse ? 0 : 1, cleanse ? 2 : 0, cleanse ? 15 : 0, cleanse ? -8 : -10, 0,
                GameEventTypes.SHOP_PURCHASED, sourceId);
    }

    private static Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String stringValue(Map<String, Object> payload, String key1, String key2, String fallback) {
        Object value = payload.get(key1);
        if (value == null) value = payload.get(key2);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private static String firstString(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null && !String.valueOf(value).isBlank()) return String.valueOf(value);
        }
        return null;
    }

    private static int intValue(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof Number number) return number.intValue();
            if (value != null && !String.valueOf(value).isBlank()) {
                try {
                    return Integer.parseInt(String.valueOf(value));
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }
}

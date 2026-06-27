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
        switch (event.getEventType()) {
            case GameEventTypes.ANSWER_CORRECT -> profileService.updateProfileFromSubmission(
                    studentNo, courseCode, true, stringValue(payload, "task_type", "taskType", "quiz"));
            case GameEventTypes.ANSWER_WRONG -> profileService.updateProfileFromSubmission(
                    studentNo, courseCode, false, stringValue(payload, "task_type", "taskType", "quiz"));
            case GameEventTypes.FLOOR_CLEARED -> profileService.addGrowth(studentNo, courseCode, 80,
                    GameEventTypes.FLOOR_CLEARED, event.getSourceId() == null ? "" : event.getSourceId());
            case GameEventTypes.BOSS_DEFEATED -> profileService.addGrowth(studentNo, courseCode, 250,
                    GameEventTypes.BOSS_DEFEATED, event.getSourceId() == null ? "" : event.getSourceId());
            case GameEventTypes.ELITE_DEFEATED -> profileService.addGrowth(studentNo, courseCode, 80,
                    GameEventTypes.ELITE_DEFEATED, event.getSourceId() == null ? "" : event.getSourceId());
            case GameEventTypes.SUPPLY_USED -> profileService.addGrowth(studentNo, courseCode, -10,
                    GameEventTypes.SUPPLY_USED, stringValue(payload, "supply_type", "supplyType", ""));
            case GameEventTypes.HINT_USED -> profileService.addGrowth(studentNo, courseCode, -5,
                    GameEventTypes.HINT_USED, stringValue(payload, "question_id", "questionId", ""));
            default -> { }
        }
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
}

package com.neu.CoursePlatform.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 游戏事件发布器 — 所有模块通过此组件发送游戏事件。
 * 使用 GameEventTypes 常量确保事件名一致。
 *
 * 在 game_mode_enabled=false 时不发送任何事件。
 */
@Component
public class GameEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(GameEventPublisher.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${game.mode.enabled:false}")
    private boolean gameModeEnabled;

    @Value("${game.event.risk-url:http://localhost:8080/practical-training/api/risk-alerts}")
    private String riskAlertsUrl;

    @Value("${game.event.profile-url:http://localhost:8080/practical-training/api/profile/event/receive}")
    private String profileEventUrl;

    /** 发送答题事件（模块3 → 模块4） */
    public void publishAnswer(String studentId, String courseId, boolean correct,
                               String taskType, String abilityPointId) {
        if (!gameModeEnabled) return;
        String eventType = correct ? GameEventTypes.ANSWER_CORRECT : GameEventTypes.ANSWER_WRONG;
        Map<String, Object> body = Map.of(
            "studentNo", studentId,
            "courseCode", courseId,
            "eventType", eventType,
            "taskType", taskType,
            "abilityPointId", abilityPointId != null ? abilityPointId : ""
        );
        postJson(profileEventUrl, body);
    }

    /** 发送楼层通关事件（模块1 → 模块4） */
    public void publishFloorCleared(String studentId, String courseId, String floor) {
        if (!gameModeEnabled) return;
        Map<String, Object> body = Map.of(
            "studentNo", studentId,
            "courseCode", courseId,
            "eventType", GameEventTypes.FLOOR_CLEARED,
            "floor", floor
        );
        postJson(profileEventUrl, body);
    }

    /** 发送Boss战事件（模块3 → 模块4） */
    public void publishBossDefeated(String studentId, String courseId) {
        if (!gameModeEnabled) return;
        Map<String, Object> body = Map.of(
            "studentNo", studentId,
            "courseCode", courseId,
            "eventType", GameEventTypes.BOSS_DEFEATED
        );
        postJson(profileEventUrl, body);
    }

    /** 发送补给使用事件（模块2 → 模块4） */
    public void publishSupplyUsed(String studentId, String courseId, String supplyType) {
        if (!gameModeEnabled) return;
        Map<String, Object> body = Map.of(
            "studentNo", studentId,
            "courseCode", courseId,
            "eventType", GameEventTypes.SUPPLY_USED,
            "supplyType", supplyType
        );
        postJson(profileEventUrl, body);
    }

    /** 发送HP严重事件（模块4 → 模块5） */
    public void publishHpCritical(String studentId, String courseId, int currentHp) {
        if (!gameModeEnabled) return;
        Map<String, Object> body = Map.of(
            "student_id", studentId,
            "course_id", courseId,
            "risk_type", GameEventTypes.HP_CRITICAL,
            "detail_json", "{\"hp\":" + currentHp + ",\"threshold\":30}"
        );
        postJson(riskAlertsUrl, body);
    }

    /** 发送卡顿检测事件（模块4 → 模块5） */
    public void publishStuckDetected(String studentId, String courseId, String knowledgePoint) {
        if (!gameModeEnabled) return;
        Map<String, Object> body = Map.of(
            "student_id", studentId,
            "course_id", courseId,
            "risk_type", GameEventTypes.STUCK_DETECTED,
            "detail_json", "{\"knowledge_point\":\"" + knowledgePoint + "\",\"consecutive_fails\":3}"
        );
        postJson(riskAlertsUrl, body);
    }

    private void postJson(String url, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            log.warn("Game event publish failed to {}: {}", url, e.getMessage());
        }
    }
}

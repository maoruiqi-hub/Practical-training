package com.neu.CoursePlatform.common.game;

import java.time.LocalDateTime;
import java.util.Map;

/** Cross-module event contract consumed by the future Module 4 game engine. */
public record TowerGameEvent(
        String eventId,
        String eventType,
        String studentId,
        String courseId,
        String sourceModule,
        String sourceId,
        LocalDateTime occurredAt,
        Map<String, Object> payload) {
}

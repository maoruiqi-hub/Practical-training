package com.neu.CoursePlatform.common.event;

import java.time.LocalDateTime;

public record ResourceViewEvent(
        String studentNo,
        String courseCode,
        String resourceId,
        String knowledgePointId,
        String action,
        Long durationMs,
        LocalDateTime occurredAt) {
}

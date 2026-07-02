package com.neu.CoursePlatform.common.event;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 跨模块爬塔事件的统一消息体。事件类型必须来自 GameEventTypes。
 */
@Value
@Builder
public class GameEvent {
    String eventId;
    String eventType;
    String studentId;
    String courseId;
    String sourceId;
    LocalDateTime occurredAt;
    Map<String, Object> payload;
}

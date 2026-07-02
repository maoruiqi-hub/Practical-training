package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.common.SharedIds;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.common.event.GameEventPublisher;
import com.neu.CoursePlatform.entity.KnowledgePointFloorStatus;
import com.neu.CoursePlatform.mapper.KnowledgePointFloorStatusMapper;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import com.neu.CoursePlatform.service.FloorProgressService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class FloorProgressServiceImpl implements FloorProgressService {
    private final KnowledgePointFloorStatusMapper mapper;
    private final CourseGameConfigService gameConfigService;
    private final GameEventPublisher eventPublisher;

    public FloorProgressServiceImpl(KnowledgePointFloorStatusMapper mapper, CourseGameConfigService gameConfigService,
                                    GameEventPublisher eventPublisher) {
        this.mapper = mapper;
        this.gameConfigService = gameConfigService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void recordQuizResult(String studentId, String courseId, String knowledgePointId,
                                 String sourceId, boolean correct, int maxScore) {
        if (!correct || maxScore <= 0 || !gameConfigService.isEnabled(courseId)) return;
        KnowledgePointFloorStatus current = find(studentId, courseId, knowledgePointId);
        if (current != null && "cleared".equals(current.getStatus())) return;
        updateFloorStatus(studentId, courseId, knowledgePointId, "cleared");
        eventPublisher.publish(GameEvent.builder().eventId(SharedIds.newId())
                .eventType(GameEventTypes.FLOOR_CLEARED).studentId(studentId).courseId(courseId)
                .sourceId(sourceId).occurredAt(LocalDateTime.now())
                .payload(Map.of("knowledge_point_id", knowledgePointId, "correct_rate", 1.0D,
                        "is_perfect", true, "time_total_ms", 0)).build());
    }

    @Override
    public boolean updateFloorStatus(String studentId, String courseId, String knowledgePointId, String status) {
        if (studentId == null || courseId == null || knowledgePointId == null || status == null) return false;
        KnowledgePointFloorStatus item = find(studentId, courseId, knowledgePointId);
        LocalDateTime now = LocalDateTime.now();
        if (item == null) {
            item = new KnowledgePointFloorStatus();
            item.setId(SharedIds.newId());
            item.setStudentId(studentId);
            item.setCourseId(courseId);
            item.setKnowledgePointId(knowledgePointId);
            item.setStatus(status);
            item.setUpdatedAt(now);
            if ("cleared".equals(status)) item.setClearedAt(now);
            return mapper.insert(item) > 0;
        }
        item.setStatus(status);
        item.setUpdatedAt(now);
        if ("cleared".equals(status)) item.setClearedAt(now);
        return mapper.updateById(item) > 0;
    }

    @EventListener
    public void onGameEvent(GameEvent event) {
        if (event == null || event.getStudentId() == null || event.getCourseId() == null
                || event.getEventType() == null || !gameConfigService.isEnabled(event.getCourseId())) {
            return;
        }
        String knowledgePointId = knowledgePointId(event);
        if (knowledgePointId == null || knowledgePointId.isBlank()) return;

        switch (event.getEventType()) {
            case GameEventTypes.FLOOR_CLEARED, GameEventTypes.ELITE_DEFEATED, GameEventTypes.BOSS_DEFEATED ->
                    updateFloorStatus(event.getStudentId(), event.getCourseId(), knowledgePointId, "cleared");
            case GameEventTypes.FLOOR_FAILED ->
                    updateFloorStatus(event.getStudentId(), event.getCourseId(), knowledgePointId, "weak");
            default -> { }
        }
    }

    private static String knowledgePointId(GameEvent event) {
        Map<String, Object> payload = event.getPayload() == null ? Map.of() : event.getPayload();
        Object value = payload.get("knowledge_point_id");
        if (value == null) value = payload.get("knowledgePointId");
        if (value == null) value = payload.get("target_kp_id");
        if (value == null) value = event.getSourceId();
        return value == null ? null : String.valueOf(value);
    }

    private KnowledgePointFloorStatus find(String studentId, String courseId, String knowledgePointId) {
        return mapper.selectOne(new LambdaQueryWrapper<KnowledgePointFloorStatus>()
                .eq(KnowledgePointFloorStatus::getStudentId, studentId)
                .eq(KnowledgePointFloorStatus::getCourseId, courseId)
                .eq(KnowledgePointFloorStatus::getKnowledgePointId, knowledgePointId));
    }
}

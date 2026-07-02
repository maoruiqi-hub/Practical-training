package com.neu.CoursePlatform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.common.event.GameEventPublisher;
import com.neu.CoursePlatform.entity.KnowledgePointFloorStatus;
import com.neu.CoursePlatform.mapper.KnowledgePointFloorStatusMapper;
import com.neu.CoursePlatform.service.CourseGameConfigService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Map;

class FloorProgressServiceImplTest {

    private FloorProgressServiceImpl service;
    private KnowledgePointFloorStatusMapper mapper;
    private CourseGameConfigService gameConfigService;
    private GameEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        mapper = mock(KnowledgePointFloorStatusMapper.class);
        gameConfigService = mock(CourseGameConfigService.class);
        eventPublisher = mock(GameEventPublisher.class);
        service = new FloorProgressServiceImpl(mapper, gameConfigService, eventPublisher);
    }

    // ============ updateFloorStatus ============

    @Test
    void updateFloorStatusInsertsWhenNotFound() {
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(KnowledgePointFloorStatus.class))).thenReturn(1);

        boolean result = service.updateFloorStatus("s1", "CS101", "kp-1", "cleared");

        assertTrue(result);
        ArgumentCaptor<KnowledgePointFloorStatus> captor = ArgumentCaptor.forClass(KnowledgePointFloorStatus.class);
        verify(mapper).insert(captor.capture());
        KnowledgePointFloorStatus inserted = captor.getValue();
        assertEquals("s1", inserted.getStudentId());
        assertEquals("CS101", inserted.getCourseId());
        assertEquals("kp-1", inserted.getKnowledgePointId());
        assertEquals("cleared", inserted.getStatus());
        assertNotNull(inserted.getClearedAt());
    }

    @Test
    void updateFloorStatusUpdatesWhenFound() {
        KnowledgePointFloorStatus existing = new KnowledgePointFloorStatus();
        existing.setId("f1");
        existing.setStudentId("s1");
        existing.setCourseId("CS101");
        existing.setKnowledgePointId("kp-1");
        existing.setStatus("weak");
        when(mapper.selectOne(any())).thenReturn(existing);
        when(mapper.updateById(any(KnowledgePointFloorStatus.class))).thenReturn(1);

        boolean result = service.updateFloorStatus("s1", "CS101", "kp-1", "cleared");

        assertTrue(result);
        verify(mapper).updateById(existing);
        assertEquals("cleared", existing.getStatus());
        assertNotNull(existing.getClearedAt());
    }

    @Test
    void updateFloorStatusReturnsFalseWhenInsertFails() {
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(KnowledgePointFloorStatus.class))).thenReturn(0);

        boolean result = service.updateFloorStatus("s1", "CS101", "kp-1", "weak");
        assertFalse(result);
    }

    @Test
    void updateFloorStatusReturnsFalseWhenUpdateFails() {
        KnowledgePointFloorStatus existing = new KnowledgePointFloorStatus();
        existing.setId("f1");
        when(mapper.selectOne(any())).thenReturn(existing);
        when(mapper.updateById(any(KnowledgePointFloorStatus.class))).thenReturn(0);

        boolean result = service.updateFloorStatus("s1", "CS101", "kp-1", "weak");
        assertFalse(result);
    }

    @Test
    void updateFloorStatusReturnsFalseWhenStudentIdNull() {
        assertFalse(service.updateFloorStatus(null, "CS101", "kp-1", "weak"));
    }

    @Test
    void updateFloorStatusReturnsFalseWhenCourseIdNull() {
        assertFalse(service.updateFloorStatus("s1", null, "kp-1", "weak"));
    }

    @Test
    void updateFloorStatusReturnsFalseWhenKnowledgePointIdNull() {
        assertFalse(service.updateFloorStatus("s1", "CS101", null, "weak"));
    }

    @Test
    void updateFloorStatusReturnsFalseWhenStatusNull() {
        assertFalse(service.updateFloorStatus("s1", "CS101", "kp-1", null));
    }

    @Test
    void updateFloorStatusDoesNotSetClearedAtForNonClearedStatus() {
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(KnowledgePointFloorStatus.class))).thenReturn(1);

        service.updateFloorStatus("s1", "CS101", "kp-1", "weak");

        ArgumentCaptor<KnowledgePointFloorStatus> captor = ArgumentCaptor.forClass(KnowledgePointFloorStatus.class);
        verify(mapper).insert(captor.capture());
        assertNull(captor.getValue().getClearedAt());
    }

    // ============ recordQuizResult ============

    @Test
    void recordQuizResultNoOpWhenWrong() {
        service.recordQuizResult("s1", "CS101", "kp-1", "src-1", false, 10);
        verifyNoInteractions(mapper);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void recordQuizResultNoOpWhenMaxScoreZero() {
        service.recordQuizResult("s1", "CS101", "kp-1", "src-1", true, 0);
        verifyNoInteractions(mapper);
    }

    @Test
    void recordQuizResultNoOpWhenGameDisabled() {
        when(gameConfigService.isEnabled("CS101")).thenReturn(false);
        service.recordQuizResult("s1", "CS101", "kp-1", "src-1", true, 10);
        verify(mapper, never()).selectOne(any());
    }

    @Test
    void recordQuizResultNoOpWhenAlreadyCleared() {
        when(gameConfigService.isEnabled("CS101")).thenReturn(true);
        KnowledgePointFloorStatus existing = new KnowledgePointFloorStatus();
        existing.setStatus("cleared");
        when(mapper.selectOne(any())).thenReturn(existing);

        service.recordQuizResult("s1", "CS101", "kp-1", "src-1", true, 10);

        verify(mapper).selectOne(any());
        verify(mapper, never()).insert(any(KnowledgePointFloorStatus.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void recordQuizResultClearsAndPublishesEvent() {
        when(gameConfigService.isEnabled("CS101")).thenReturn(true);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(KnowledgePointFloorStatus.class))).thenReturn(1);

        service.recordQuizResult("s1", "CS101", "kp-1", "src-1", true, 10);

        verify(mapper).insert(any(KnowledgePointFloorStatus.class));
        verify(eventPublisher).publish(any(GameEvent.class));
    }

    // ============ onGameEvent ============

    @Test
    void onGameEventNoOpWhenEventNull() {
        service.onGameEvent(null);
        verifyNoInteractions(mapper);
    }

    @Test
    void onGameEventNoOpWhenStudentIdNull() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.FLOOR_CLEARED)
                .courseId("CS101").build();
        service.onGameEvent(event);
        verifyNoInteractions(mapper);
    }

    @Test
    void onGameEventNoOpWhenCourseIdNull() {
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.FLOOR_CLEARED)
                .studentId("s1").build();
        service.onGameEvent(event);
        verifyNoInteractions(mapper);
    }

    @Test
    void onGameEventNoOpWhenGameDisabled() {
        when(gameConfigService.isEnabled("CS101")).thenReturn(false);
        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.FLOOR_CLEARED)
                .studentId("s1").courseId("CS101")
                .payload(Map.of("knowledge_point_id", "kp-1")).build();

        service.onGameEvent(event);
        verify(mapper, never()).selectOne(any());
    }

    @Test
    void onGameEventFloorClearedUpdatesStatus() {
        when(gameConfigService.isEnabled("CS101")).thenReturn(true);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(KnowledgePointFloorStatus.class))).thenReturn(1);

        GameEvent event = GameEvent.builder()
                .eventId("e1").eventType(GameEventTypes.FLOOR_CLEARED)
                .studentId("s1").courseId("CS101")
                .payload(Map.of("knowledge_point_id", "kp-1")).build();

        service.onGameEvent(event);

        ArgumentCaptor<KnowledgePointFloorStatus> captor = ArgumentCaptor.forClass(KnowledgePointFloorStatus.class);
        verify(mapper).insert(captor.capture());
        assertEquals("cleared", captor.getValue().getStatus());
    }

    @Test
    void onGameEventEliteDefeatedUpdatesStatus() {
        when(gameConfigService.isEnabled("CS101")).thenReturn(true);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(KnowledgePointFloorStatus.class))).thenReturn(1);

        GameEvent event = GameEvent.builder()
                .eventId("e2").eventType(GameEventTypes.ELITE_DEFEATED)
                .studentId("s1").courseId("CS101")
                .payload(Map.of("knowledge_point_id", "kp-2")).build();

        service.onGameEvent(event);

        ArgumentCaptor<KnowledgePointFloorStatus> captor = ArgumentCaptor.forClass(KnowledgePointFloorStatus.class);
        verify(mapper).insert(captor.capture());
        assertEquals("cleared", captor.getValue().getStatus());
    }

    @Test
    void onGameEventBossDefeatedUpdatesStatus() {
        when(gameConfigService.isEnabled("CS101")).thenReturn(true);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(KnowledgePointFloorStatus.class))).thenReturn(1);

        GameEvent event = GameEvent.builder()
                .eventId("e3").eventType(GameEventTypes.BOSS_DEFEATED)
                .studentId("s1").courseId("CS101")
                .payload(Map.of("knowledge_point_id", "kp-3")).build();

        service.onGameEvent(event);

        ArgumentCaptor<KnowledgePointFloorStatus> captor = ArgumentCaptor.forClass(KnowledgePointFloorStatus.class);
        verify(mapper).insert(captor.capture());
        assertEquals("cleared", captor.getValue().getStatus());
    }

    @Test
    void onGameEventFloorFailedSetsWeak() {
        when(gameConfigService.isEnabled("CS101")).thenReturn(true);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(KnowledgePointFloorStatus.class))).thenReturn(1);

        GameEvent event = GameEvent.builder()
                .eventId("e4").eventType(GameEventTypes.FLOOR_FAILED)
                .studentId("s1").courseId("CS101")
                .payload(Map.of("knowledge_point_id", "kp-1")).build();

        service.onGameEvent(event);

        ArgumentCaptor<KnowledgePointFloorStatus> captor = ArgumentCaptor.forClass(KnowledgePointFloorStatus.class);
        verify(mapper).insert(captor.capture());
        assertEquals("weak", captor.getValue().getStatus());
    }

    @Test
    void onGameEventUnknownTypeNoOp() {
        when(gameConfigService.isEnabled("CS101")).thenReturn(true);

        GameEvent event = GameEvent.builder()
                .eventId("e5").eventType("UNKNOWN_TYPE")
                .studentId("s1").courseId("CS101")
                .payload(Map.of("knowledge_point_id", "kp-1")).build();

        service.onGameEvent(event);
        verify(mapper, never()).insert(any(KnowledgePointFloorStatus.class));
    }

    @Test
    void onGameEventUsesKnowledgePointIdFromPayload() {
        when(gameConfigService.isEnabled("CS101")).thenReturn(true);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(KnowledgePointFloorStatus.class))).thenReturn(1);

        GameEvent event = GameEvent.builder()
                .eventId("e6").eventType(GameEventTypes.FLOOR_CLEARED)
                .studentId("s1").courseId("CS101")
                .payload(Map.of("knowledgePointId", "kp-camel")).build();

        service.onGameEvent(event);

        ArgumentCaptor<KnowledgePointFloorStatus> captor = ArgumentCaptor.forClass(KnowledgePointFloorStatus.class);
        verify(mapper).insert(captor.capture());
        assertEquals("kp-camel", captor.getValue().getKnowledgePointId());
    }

    @Test
    void onGameEventUsesTargetKpIdFallback() {
        when(gameConfigService.isEnabled("CS101")).thenReturn(true);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(KnowledgePointFloorStatus.class))).thenReturn(1);

        GameEvent event = GameEvent.builder()
                .eventId("e7").eventType(GameEventTypes.FLOOR_CLEARED)
                .studentId("s1").courseId("CS101")
                .payload(Map.of("target_kp_id", "kp-target")).build();

        service.onGameEvent(event);

        ArgumentCaptor<KnowledgePointFloorStatus> captor = ArgumentCaptor.forClass(KnowledgePointFloorStatus.class);
        verify(mapper).insert(captor.capture());
        assertEquals("kp-target", captor.getValue().getKnowledgePointId());
    }

    @Test
    void onGameEventUsesSourceIdFallback() {
        when(gameConfigService.isEnabled("CS101")).thenReturn(true);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(KnowledgePointFloorStatus.class))).thenReturn(1);

        GameEvent event = GameEvent.builder()
                .eventId("e8").eventType(GameEventTypes.FLOOR_CLEARED)
                .studentId("s1").courseId("CS101")
                .sourceId("src-fallback").build();

        service.onGameEvent(event);

        ArgumentCaptor<KnowledgePointFloorStatus> captor = ArgumentCaptor.forClass(KnowledgePointFloorStatus.class);
        verify(mapper).insert(captor.capture());
        assertEquals("src-fallback", captor.getValue().getKnowledgePointId());
    }

    @Test
    void onGameEventNoOpWhenKnowledgePointIdBlank() {
        when(gameConfigService.isEnabled("CS101")).thenReturn(true);

        GameEvent event = GameEvent.builder()
                .eventId("e9").eventType(GameEventTypes.FLOOR_CLEARED)
                .studentId("s1").courseId("CS101")
                .payload(Map.of("knowledge_point_id", "  ")).build();

        service.onGameEvent(event);
        verify(mapper, never()).insert(any(KnowledgePointFloorStatus.class));
    }
}

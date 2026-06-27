package com.neu.CoursePlatform.module5_analytics.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.module5_analytics.entity.RiskAlert;
import com.neu.CoursePlatform.module5_analytics.service.ClassInfoService;
import com.neu.CoursePlatform.module5_analytics.service.RiskAlertService;
import com.neu.CoursePlatform.module5_analytics.service.RiskDetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAlertControllerTest {

    @Mock
    private RiskAlertService riskAlertService;

    @Mock
    private RiskDetectionService riskDetectionService;

    @Mock
    private ClassInfoService classInfoService;

    @Mock
    private Auth auth;

    @InjectMocks
    private RiskAlertController controller;

    @Test
    void hpCriticalGameEventCreatesHighRiskAlert() {
        when(riskAlertService.receiveEvent("student-1", "course-1",
                GameEventTypes.HP_CRITICAL, "high", "{hp=20}")).thenReturn(new RiskAlert());

        controller.receiveGameEvent(GameEvent.builder()
                .eventType(GameEventTypes.HP_CRITICAL)
                .studentId("student-1")
                .courseId("course-1")
                .occurredAt(LocalDateTime.now())
                .payload(Map.of("hp", 20))
                .build());

        verify(riskAlertService).receiveEvent("student-1", "course-1",
                GameEventTypes.HP_CRITICAL, "high", "{hp=20}");
    }

    @Test
    void stuckGameEventCreatesMediumRiskAlert() {
        controller.receiveGameEvent(GameEvent.builder()
                .eventType(GameEventTypes.STUCK_DETECTED)
                .studentId("student-1")
                .courseId("course-1")
                .occurredAt(LocalDateTime.now())
                .payload(Map.of("knowledge_point", "loop"))
                .build());

        verify(riskAlertService).receiveEvent(eq("student-1"), eq("course-1"),
                eq(GameEventTypes.STUCK_DETECTED), eq("medium"), contains("loop"));
    }
}

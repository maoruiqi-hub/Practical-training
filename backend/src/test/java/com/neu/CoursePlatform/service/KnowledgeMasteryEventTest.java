package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.dto.KnowledgeMasteryUpdateRequest;
import com.neu.CoursePlatform.entity.KnowledgeMastery;
import com.neu.CoursePlatform.service.impl.KnowledgeMasteryServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KnowledgeMasteryEventTest {

    @Test
    void answerEventIsConvertedToMasteryUpdate() {
        CapturingKnowledgeMasteryService service = new CapturingKnowledgeMasteryService();
        service.handleAssessmentResult(GameEvent.builder()
                .eventType(GameEventTypes.ANSWER_CORRECT)
                .studentId("student-1")
                .courseId("course-1")
                .sourceId("answer-1")
                .occurredAt(LocalDateTime.now())
                .payload(Map.of("knowledge_point_id", "kp-1"))
                .build());

        assertEquals("student-1", service.captured.getStudentNo());
        assertEquals("course-1", service.captured.getCourseCode());
        assertEquals("kp-1", service.captured.getKnowledgePointId());
        assertEquals(100, service.captured.getMasteryScore());
        assertEquals("assessment", service.captured.getSourceType());
        assertEquals("answer-1", service.captured.getSourceId());
    }

    @Test
    void unrelatedEventIsIgnored() {
        CapturingKnowledgeMasteryService service = new CapturingKnowledgeMasteryService();
        service.handleAssessmentResult(GameEvent.builder()
                .eventType(GameEventTypes.BOSS_DEFEATED)
                .studentId("student-1")
                .courseId("course-1")
                .sourceId("submission-1")
                .occurredAt(LocalDateTime.now())
                .payload(Map.of("knowledge_point_id", "kp-1"))
                .build());

        assertNull(service.captured);
    }

    private static class CapturingKnowledgeMasteryService extends KnowledgeMasteryServiceImpl {
        private KnowledgeMasteryUpdateRequest captured;

        CapturingKnowledgeMasteryService() {
            super(null, null);
        }

        @Override
        public KnowledgeMastery upsert(KnowledgeMasteryUpdateRequest request) {
            this.captured = request;
            KnowledgeMastery mastery = new KnowledgeMastery();
            mastery.setStudentNo(request.getStudentNo());
            mastery.setCourseCode(request.getCourseCode());
            mastery.setKnowledgePointId(request.getKnowledgePointId());
            mastery.setMasteryScore(request.getMasteryScore());
            return mastery;
        }
    }
}

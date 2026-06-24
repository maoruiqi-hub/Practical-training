package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgeRelation;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.KnowledgeRelationService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeRelationControllerTest {

    @Mock
    private KnowledgeRelationService knowledgeRelationService;

    @Mock
    private KnowledgePointService knowledgePointService;

    @Mock
    private Auth auth;

    @Mock
    private HttpSession session;

    @InjectMocks
    private KnowledgeRelationController knowledgeRelationController;

    @Test
    void authorizedTeacherCanCreatePrerequisiteRelation() {
        KnowledgeRelation relation = relation("1", "2", "PREREQUISITE");
        when(knowledgePointService.getById("1")).thenReturn(point("1", "1"));
        when(knowledgePointService.getById("2")).thenReturn(point("2", "1"));
        when(auth.canModifyCourse(session, "1")).thenReturn(true);
        when(knowledgeRelationService.relationExists("1", "1", "2", "prerequisite")).thenReturn(false);
        when(knowledgeRelationService.wouldCreateCycle("1", "1", "2", "prerequisite")).thenReturn(false);

        Result<String> result = knowledgeRelationController.create(relation, session);

        ArgumentCaptor<KnowledgeRelation> captor = ArgumentCaptor.forClass(KnowledgeRelation.class);
        verify(knowledgeRelationService).save(captor.capture());
        assertEquals(200, result.getCode());
        assertEquals("1", captor.getValue().getCourseCode());
        assertEquals("prerequisite", captor.getValue().getRelationType());
    }

    @Test
    void crossCourseRelationIsRejected() {
        KnowledgeRelation relation = relation("1", "2", "related");
        when(knowledgePointService.getById("1")).thenReturn(point("1", "1"));
        when(knowledgePointService.getById("2")).thenReturn(point("2", "2"));

        Result<String> result = knowledgeRelationController.create(relation, session);

        assertEquals(500, result.getCode());
        verify(knowledgeRelationService, never()).save(any(KnowledgeRelation.class));
    }

    @Test
    void selfRelationIsRejected() {
        Result<String> result = knowledgeRelationController.create(relation("1", "1", "hierarchy"), session);

        assertEquals(500, result.getCode());
        verify(knowledgePointService, never()).getById("1");
    }

    @Test
    void cyclicRelationIsRejected() {
        KnowledgeRelation relation = relation("1", "2", "hierarchy");
        when(knowledgePointService.getById("1")).thenReturn(point("1", "1"));
        when(knowledgePointService.getById("2")).thenReturn(point("2", "1"));
        when(auth.canModifyCourse(session, "1")).thenReturn(true);
        when(knowledgeRelationService.relationExists("1", "1", "2", "hierarchy")).thenReturn(false);
        when(knowledgeRelationService.wouldCreateCycle("1", "1", "2", "hierarchy")).thenReturn(true);

        Result<String> result = knowledgeRelationController.create(relation, session);

        assertEquals(500, result.getCode());
        verify(knowledgeRelationService, never()).save(any(KnowledgeRelation.class));
    }

    private KnowledgePoint point(String id, String courseCode) {
        KnowledgePoint point = new KnowledgePoint();
        point.setKnowledgePointId(id);
        point.setCourseCode(courseCode);
        return point;
    }

    private KnowledgeRelation relation(String from, String to, String type) {
        KnowledgeRelation relation = new KnowledgeRelation();
        relation.setFromKnowledgePointId(from);
        relation.setToKnowledgePointId(to);
        relation.setRelationType(type);
        return relation;
    }
}

package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgeRelation;
import com.neu.CoursePlatform.service.CourseService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class KnowledgePointControllerTest {

    @Mock
    private KnowledgePointService knowledgePointService;

    @Mock
    private KnowledgeRelationService knowledgeRelationService;

    @Mock
    private CourseService courseService;

    @Mock
    private Auth auth;

    @Mock
    private HttpSession session;

    @InjectMocks
    private KnowledgePointController knowledgePointController;

    @Test
    void authorizedTeacherCanCreateKnowledgePoint() {
        KnowledgePoint knowledgePoint = validKnowledgePoint();
        when(courseService.getById("1")).thenReturn(new Course());
        when(auth.canModifyCourse(session, "1")).thenReturn(true);

        Result<String> result = knowledgePointController.create(knowledgePoint, session);

        ArgumentCaptor<KnowledgePoint> captor = ArgumentCaptor.forClass(KnowledgePoint.class);
        verify(knowledgePointService).save(captor.capture());
        assertEquals(200, result.getCode());
        assertEquals("List", captor.getValue().getName());
        assertEquals("1", captor.getValue().getCourseCode());
    }

    @Test
    void knowledgePointWithoutNameIsRejected() {
        KnowledgePoint knowledgePoint = validKnowledgePoint();
        knowledgePoint.setName(" ");

        Result<String> result = knowledgePointController.create(knowledgePoint, session);

        assertEquals(500, result.getCode());
        verify(knowledgePointService, never()).save(any(KnowledgePoint.class));
    }

    @Test
    void knowledgePointCannotBeMovedAcrossCourses() {
        KnowledgePoint existing = validKnowledgePoint();
        existing.setKnowledgePointId("10");
        KnowledgePoint request = validKnowledgePoint();
        request.setCourseCode("2");
        when(knowledgePointService.getById("10")).thenReturn(existing);
        when(auth.canModifyCourse(session, "1")).thenReturn(true);

        Result<String> result = knowledgePointController.update("10", request, session);

        assertEquals(500, result.getCode());
        verify(knowledgePointService, never()).updateById(any(KnowledgePoint.class));
    }

    @Test
    void unauthorizedDeletionIsRejected() {
        KnowledgePoint existing = validKnowledgePoint();
        existing.setKnowledgePointId("10");
        when(knowledgePointService.getById("10")).thenReturn(existing);
        when(auth.canModifyCourse(session, "1")).thenReturn(false);

        Result<Void> result = knowledgePointController.delete("10", session);

        assertEquals(500, result.getCode());
        verify(knowledgePointService, never()).removeById(anyString());
        verify(knowledgePointService, never()).deleteWithDependencies(anyString());
    }

    @Test
    void authorizedDeletionCleansKnowledgePointDependencies() {
        KnowledgePoint existing = validKnowledgePoint();
        existing.setKnowledgePointId("10");
        when(knowledgePointService.getById("10")).thenReturn(existing);
        when(auth.canModifyCourse(session, "1")).thenReturn(true);

        Result<Void> result = knowledgePointController.delete("10", session);

        assertEquals(200, result.getCode());
        verify(knowledgePointService).deleteWithDependencies("10");
    }

    @Test
    void prerequisiteQueryReturnsTheWholePrerequisiteChain() {
        KnowledgePoint first = validKnowledgePoint();
        first.setKnowledgePointId("1");
        KnowledgePoint second = validKnowledgePoint();
        second.setKnowledgePointId("2");
        KnowledgePoint target = validKnowledgePoint();
        target.setKnowledgePointId("3");
        KnowledgeRelation firstToSecond = relation("1", "2");
        KnowledgeRelation secondToTarget = relation("2", "3");
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(knowledgePointService.getById("3")).thenReturn(target);
        when(knowledgePointService.listByCourseCode("1", null)).thenReturn(List.of(first, second, target));
        when(knowledgeRelationService.listByCourseCode("1")).thenReturn(List.of(firstToSecond, secondToTarget));

        Result<List<KnowledgePoint>> result = knowledgePointController.prerequisites("3", session);

        assertEquals(200, result.getCode());
        assertEquals(List.of("2", "1"), result.getData().stream().map(KnowledgePoint::getKnowledgePointId).toList());
    }

    private KnowledgePoint validKnowledgePoint() {
        KnowledgePoint knowledgePoint = new KnowledgePoint();
        knowledgePoint.setCourseCode("1");
        knowledgePoint.setName("List");
        knowledgePoint.setChapter("Chapter 1");
        knowledgePoint.setImportance(3);
        knowledgePoint.setGenerationMethod("manual");
        return knowledgePoint;
    }

    private KnowledgeRelation relation(String from, String to) {
        KnowledgeRelation relation = new KnowledgeRelation();
        relation.setCourseCode("1");
        relation.setFromKnowledgePointId(from);
        relation.setToKnowledgePointId(to);
        relation.setRelationType("prerequisite");
        return relation;
    }
}

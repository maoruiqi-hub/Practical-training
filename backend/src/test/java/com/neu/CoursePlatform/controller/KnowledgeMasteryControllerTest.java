package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.KnowledgeMasteryUpdateRequest;
import com.neu.CoursePlatform.entity.KnowledgeMastery;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.service.KnowledgeMasteryService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeMasteryControllerTest {

    @Mock
    private KnowledgeMasteryService masteryService;

    @Mock
    private Auth auth;

    @Mock
    private HttpSession session;

    @InjectMocks
    private KnowledgeMasteryController knowledgeMasteryController;

    @Test
    void studentCannotReadAnotherStudentsMastery() {
        Student currentStudent = new Student();
        currentStudent.setStudentNo("1001");
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(auth.getStudent(session)).thenReturn(currentStudent);

        Result<?> result = knowledgeMasteryController.list("1002", "1", session);

        assertEquals(500, result.getCode());
        verify(masteryService, never()).listByStudentAndCourse(anyString(), anyString());
    }

    @Test
    void studentCannotWriteMasteryThroughPublicApi() {
        KnowledgeMasteryUpdateRequest request = validRequest();
        when(auth.canModifyCourse(session, "1")).thenReturn(false);

        Result<KnowledgeMastery> result = knowledgeMasteryController.upsert(request, session);

        assertEquals(500, result.getCode());
        verify(masteryService, never()).upsert(any());
    }

    @Test
    void authorizedTeacherCanManuallyCorrectMastery() {
        KnowledgeMasteryUpdateRequest request = validRequest();
        KnowledgeMastery mastery = new KnowledgeMastery();
        mastery.setMasteryScore(90);
        when(auth.canModifyCourse(session, "1")).thenReturn(true);
        when(masteryService.validateForUpsert(request)).thenReturn(null);
        when(masteryService.upsert(request)).thenReturn(mastery);

        Result<KnowledgeMastery> result = knowledgeMasteryController.upsert(request, session);

        assertEquals(200, result.getCode());
        assertEquals(90, result.getData().getMasteryScore());
    }

    private KnowledgeMasteryUpdateRequest validRequest() {
        KnowledgeMasteryUpdateRequest request = new KnowledgeMasteryUpdateRequest();
        request.setStudentNo("1001");
        request.setCourseCode("1");
        request.setKnowledgePointId("10");
        request.setMasteryScore(90);
        request.setSourceType("manual");
        return request;
    }
}

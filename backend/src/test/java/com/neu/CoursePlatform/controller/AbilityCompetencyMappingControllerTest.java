package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.AbilityCompetencyRelationRequest;
import com.neu.CoursePlatform.entity.CompetencyPoint;
import com.neu.CoursePlatform.entity.CompetencyTaskObservation;
import com.neu.CoursePlatform.service.AbilityCompetencyMappingService;
import com.neu.CoursePlatform.service.CourseService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import java.util.List;

class AbilityCompetencyMappingControllerTest {

    @Test
    void rejectsRelationWhenObjectBelongsToAnotherCourse() {
        AbilityCompetencyMappingService mappingService = mock(AbilityCompetencyMappingService.class);
        CourseService courseService = mock(CourseService.class);
        Auth auth = mock(Auth.class);
        HttpSession session = mock(HttpSession.class);
        when(auth.canModifyCourse(session, "course-a")).thenReturn(true);
        doThrow(new IllegalArgumentException("能力点不属于当前课程"))
                .when(mappingService).saveRelation(any());
        AbilityCompetencyMappingController controller = new AbilityCompetencyMappingController(mappingService, courseService, auth);

        AbilityCompetencyRelationRequest request = new AbilityCompetencyRelationRequest();
        request.setCourseCode("course-a");
        request.setAbilityPointId("ability-from-course-b");
        request.setCompetencyId("competency-from-course-a");
        request.setRelationStatus("related");

        Result<Void> result = controller.relation(request, session);

        assertNotEquals(200, result.getCode());
        verify(mappingService).saveRelation(request);
    }

    @Test
    void rejectsObservationWhenTaskBelongsToAnotherCourse() {
        AbilityCompetencyMappingService mappingService = mock(AbilityCompetencyMappingService.class);
        CourseService courseService = mock(CourseService.class);
        Auth auth = mock(Auth.class);
        HttpSession session = mock(HttpSession.class);
        when(auth.canModifyCourse(session, "course-a")).thenReturn(true);
        doThrow(new IllegalArgumentException("观测任务不属于当前课程"))
                .when(mappingService).saveObservation(any());
        AbilityCompetencyMappingController controller = new AbilityCompetencyMappingController(mappingService, courseService, auth);

        CompetencyTaskObservation observation = new CompetencyTaskObservation();
        observation.setCourseCode("course-a");
        observation.setTaskNo("task-from-course-b");
        observation.setCompetencyId("competency-from-course-a");

        Result<Void> result = controller.observation(observation, session);

        assertNotEquals(200, result.getCode());
        verify(mappingService).saveObservation(observation);
    }

    @Test
    void updateLoadsRealCompetencyInsteadOfTrustingRequestCourse() {
        AbilityCompetencyMappingService mappingService = mock(AbilityCompetencyMappingService.class);
        CourseService courseService = mock(CourseService.class);
        Auth auth = mock(Auth.class);
        HttpSession session = mock(HttpSession.class);
        CompetencyPoint existing = new CompetencyPoint();
        existing.setCompetencyId("competency-a");
        existing.setCourseCode("course-a");
        when(mappingService.getCompetencyById("competency-a")).thenReturn(existing);
        when(auth.canModifyCourse(session, "course-a")).thenReturn(true);
        AbilityCompetencyMappingController controller = new AbilityCompetencyMappingController(mappingService, courseService, auth);

        CompetencyPoint request = new CompetencyPoint();
        request.setCourseCode("course-b");
        request.setName("伪造归属");

        Result<Void> result = controller.update("competency-a", request, session);

        assertNotEquals(200, result.getCode());
        verify(mappingService, never()).updateCompetency(eq("competency-a"), any());
    }

    @Test
    void batchObservationUsesOneTransactionalServiceCall() {
        AbilityCompetencyMappingService mappingService = mock(AbilityCompetencyMappingService.class);
        CourseService courseService = mock(CourseService.class);
        Auth auth = mock(Auth.class);
        HttpSession session = mock(HttpSession.class);
        when(auth.canModifyCourse(session, "course-a")).thenReturn(true);
        AbilityCompetencyMappingController controller = new AbilityCompetencyMappingController(mappingService, courseService, auth);

        CompetencyTaskObservation first = new CompetencyTaskObservation();
        first.setCourseCode("course-a");
        first.setTaskNo("task-1");
        first.setCompetencyId("competency-1");
        CompetencyTaskObservation second = new CompetencyTaskObservation();
        second.setCourseCode("course-a");
        second.setTaskNo("task-1");
        second.setCompetencyId("competency-2");

        Result<Void> result = controller.observationsBatch(List.of(first, second), session);

        assertEquals(200, result.getCode());
        verify(mappingService).saveObservations(List.of(first, second));
    }
}

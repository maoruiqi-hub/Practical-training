package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.service.AbilityMapService;
import com.neu.CoursePlatform.service.AbilityPointService;
import com.neu.CoursePlatform.service.CourseService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbilityMapControllerTest {

    @Mock private AbilityPointService abilityPointService;
    @Mock private AbilityMapService abilityMapService;
    @Mock private CourseService courseService;
    @Mock private KnowledgePointService knowledgePointService;
    @Mock private Auth auth;
    @Mock private HttpSession session;

    @InjectMocks private AbilityMapController controller;

    @Test
    void createRejectsSameNameIgnoringCaseAndSpaces() {
        allowCourseModification();
        when(abilityPointService.listByCourseCode("1"))
                .thenReturn(List.of(point("ap-1", " Python基础能力 ")));

        Result<String> result = controller.create(point(null, "python基础能力"), session);

        assertEquals(500, result.getCode());
        assertEquals("该课程已存在同名能力点", result.getMsg());
        verify(abilityPointService, never()).save(any());
    }

    @Test
    void createRejectsTwentyFirstAbilityPoint() {
        allowCourseModification();
        List<AbilityPoint> points = new ArrayList<>();
        for (int i = 0; i < 20; i++) points.add(point("ap-" + i, "能力点" + i));
        when(abilityPointService.listByCourseCode("1")).thenReturn(points);

        Result<String> result = controller.create(point(null, "新能力点"), session);

        assertEquals(500, result.getCode());
        assertEquals("每门课程最多只能创建20个能力点", result.getMsg());
        verify(abilityPointService, never()).save(any());
    }

    @Test
    void createAllowsUniqueNameBelowLimit() {
        allowCourseModification();
        when(abilityPointService.listByCourseCode("1"))
                .thenReturn(List.of(point("ap-1", "已有能力点")));

        Result<String> result = controller.create(point(null, " 新能力点 "), session);

        assertEquals(200, result.getCode());
        verify(abilityPointService).save(any());
    }

    @Test
    void updateRejectsAnotherAbilityPointName() {
        AbilityPoint existing = point("ap-1", "原名称");
        when(abilityPointService.getById("ap-1")).thenReturn(existing);
        when(auth.canModifyCourse(session, "1")).thenReturn(true);
        when(abilityPointService.listByCourseCode("1"))
                .thenReturn(List.of(existing, point("ap-2", "重复名称")));

        Result<Void> result = controller.update("ap-1", point(null, " 重复名称 "), session);

        assertEquals(500, result.getCode());
        assertEquals("该课程已存在同名能力点", result.getMsg());
        verify(abilityPointService, never()).updateById(any());
    }

    private void allowCourseModification() {
        Course course = new Course();
        course.setCourseCode("1");
        when(courseService.getById("1")).thenReturn(course);
        when(auth.canModifyCourse(session, "1")).thenReturn(true);
    }

    private AbilityPoint point(String id, String name) {
        AbilityPoint point = new AbilityPoint();
        point.setAbilityPointId(id);
        point.setCourseCode("1");
        point.setName(name);
        return point;
    }
}

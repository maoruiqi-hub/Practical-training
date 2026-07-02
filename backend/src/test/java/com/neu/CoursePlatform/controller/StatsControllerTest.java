package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.service.StatsService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsControllerTest {

    @Mock private StatsService statsService;
    @Mock private Auth auth;
    @Mock private HttpSession session;
    @InjectMocks private StatsController controller;

    // ============ studentStats ============

    @Test
    void studentStatsAdminCanAccess() {
        when(auth.isAdmin(session)).thenReturn(true);
        when(statsService.buildStudentStats("2024001")).thenReturn(Map.of("score", 85));

        Result<Map<String, Object>> result = controller.studentStats("2024001", session);

        assertEquals(200, result.getCode());
        assertEquals(85, result.getData().get("score"));
    }

    @Test
    void studentStatsOwnStudentCanAccess() {
        when(auth.isAdmin(session)).thenReturn(false);
        Student student = new Student();
        student.setStudentNo("2024001");
        when(session.getAttribute("student")).thenReturn(student);
        when(statsService.buildStudentStats("2024001")).thenReturn(Map.of());

        Result<Map<String, Object>> result = controller.studentStats("2024001", session);

        assertEquals(200, result.getCode());
    }

    @Test
    void studentStatsOtherStudentRejected() {
        when(auth.isAdmin(session)).thenReturn(false);
        Student student = new Student();
        student.setStudentNo("2024002");
        when(session.getAttribute("student")).thenReturn(student);

        Result<Map<String, Object>> result = controller.studentStats("2024001", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void studentStatsNotLoggedInRejected() {
        when(auth.isAdmin(session)).thenReturn(false);
        when(session.getAttribute("student")).thenReturn(null);

        Result<Map<String, Object>> result = controller.studentStats("2024001", session);

        assertEquals(500, result.getCode());
    }

    // ============ studentScores (alias) ============

    @Test
    void studentScoresDelegatesToStudentStats() {
        when(auth.isAdmin(session)).thenReturn(true);
        when(statsService.buildStudentStats("2024001")).thenReturn(Map.of("scores", List.of(90, 80)));

        Result<Map<String, Object>> result = controller.studentScores("2024001", session);

        assertEquals(200, result.getCode());
    }

    // ============ studentCourseStats ============

    @Test
    void studentCourseStatsOwnStudentCanAccess() {
        when(auth.isAdmin(session)).thenReturn(false);
        Student student = new Student();
        student.setStudentNo("2024001");
        when(session.getAttribute("student")).thenReturn(student);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("courseName", "Java编程");
        when(statsService.buildStudentCourseStats("2024001", "CS101")).thenReturn(stats);

        Result<Map<String, Object>> result = controller.studentCourseStats("2024001", "CS101", session);

        assertEquals(200, result.getCode());
        assertEquals("Java编程", result.getData().get("courseName"));
    }

    @Test
    void studentCourseStatsOtherStudentRejected() {
        when(auth.isAdmin(session)).thenReturn(false);
        Student student = new Student();
        student.setStudentNo("2024002");
        when(session.getAttribute("student")).thenReturn(student);

        Result<Map<String, Object>> result = controller.studentCourseStats("2024001", "CS101", session);

        assertEquals(500, result.getCode());
    }

    // ============ courseStats ============

    @Test
    void courseStatsRequiresCoursePermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<Map<String, Object>> result = controller.courseStats("CS101", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void courseStatsReturnsData() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("avgScore", 75.5);
        when(statsService.buildCourseStats("CS101")).thenReturn(stats);

        Result<Map<String, Object>> result = controller.courseStats("CS101", session);

        assertEquals(200, result.getCode());
        assertEquals(75.5, result.getData().get("avgScore"));
    }
}

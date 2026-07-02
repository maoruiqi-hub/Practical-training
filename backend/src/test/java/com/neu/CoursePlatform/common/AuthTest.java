package com.neu.CoursePlatform.common;

import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthTest {

    private Auth auth;
    private CourseService courseService;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        courseService = mock(CourseService.class);
        auth = new Auth(courseService);
        session = new MockHttpSession();
    }

    @Test
    void isLoggedInShouldReturnFalseWhenSessionEmpty() {
        assertFalse(auth.isLoggedIn(session));
    }

    @Test
    void isLoggedInShouldReturnTrueWhenStudentPresent() {
        session.setAttribute("student", new Student());
        assertTrue(auth.isLoggedIn(session));
    }

    @Test
    void isLoggedInShouldReturnTrueWhenTeacherPresent() {
        session.setAttribute("teacher", new Teacher());
        assertTrue(auth.isLoggedIn(session));
    }

    @Test
    void isAdminShouldReturnTrueForAdminRole() {
        Teacher teacher = new Teacher();
        teacher.setRole("admin");
        session.setAttribute("teacher", teacher);
        assertTrue(auth.isAdmin(session));
    }

    @Test
    void isAdminShouldReturnFalseForTeacherRole() {
        Teacher teacher = new Teacher();
        teacher.setRole("teacher");
        session.setAttribute("teacher", teacher);
        assertFalse(auth.isAdmin(session));
    }

    @Test
    void isAdminShouldReturnFalseWhenNotLoggedIn() {
        assertFalse(auth.isAdmin(session));
    }

    @Test
    void isTeacherShouldReturnTrueForTeacherRole() {
        Teacher teacher = new Teacher();
        teacher.setRole("teacher");
        session.setAttribute("teacher", teacher);
        assertTrue(auth.isTeacher(session));
    }

    @Test
    void isTeacherShouldReturnFalseForAdminRole() {
        Teacher teacher = new Teacher();
        teacher.setRole("admin");
        session.setAttribute("teacher", teacher);
        assertFalse(auth.isTeacher(session));
    }

    @Test
    void getTeacherShouldReturnNullWhenNotLoggedIn() {
        assertNull(auth.getTeacher(session));
    }

    @Test
    void getTeacherShouldReturnTeacherFromSession() {
        Teacher teacher = new Teacher();
        teacher.setTeacherNo("T001");
        session.setAttribute("teacher", teacher);
        assertEquals(teacher, auth.getTeacher(session));
    }

    @Test
    void getStudentShouldReturnNullWhenNotLoggedIn() {
        assertNull(auth.getStudent(session));
    }

    @Test
    void getStudentShouldReturnStudentFromSession() {
        Student student = new Student();
        student.setStudentNo("S001");
        session.setAttribute("student", student);
        assertEquals(student, auth.getStudent(session));
    }

    @Test
    void getTeacherIdShouldReturnStringValue() {
        Teacher teacher = new Teacher();
        teacher.setTeacherNo("T001");
        session.setAttribute("teacher", teacher);
        assertEquals("T001", auth.getTeacherId(session));
    }

    @Test
    void getTeacherIdShouldReturnNullWhenNotLoggedIn() {
        assertNull(auth.getTeacherId(session));
    }

    @Test
    void canModifyCourseShouldReturnFalseWhenNotLoggedIn() {
        assertFalse(auth.canModifyCourse(session, "CS101"));
    }

    @Test
    void canModifyCourseShouldReturnTrueForAdmin() {
        Teacher teacher = new Teacher();
        teacher.setRole("admin");
        session.setAttribute("teacher", teacher);
        assertTrue(auth.canModifyCourse(session, "CS101"));
    }

    @Test
    void canModifyCourseShouldReturnTrueWhenTeacherNoMatches() {
        Teacher teacher = new Teacher();
        teacher.setRole("teacher");
        teacher.setTeacherNo("T001");
        session.setAttribute("teacher", teacher);
        Course course = new Course();
        course.setTeacherNo("T001");
        when(courseService.getById("CS101")).thenReturn(course);
        assertTrue(auth.canModifyCourse(session, "CS101"));
    }

    @Test
    void canModifyCourseShouldReturnFalseWhenTeacherNoMismatch() {
        Teacher teacher = new Teacher();
        teacher.setRole("teacher");
        teacher.setTeacherNo("T001");
        session.setAttribute("teacher", teacher);
        Course course = new Course();
        course.setTeacherNo("T002");
        when(courseService.getById("CS101")).thenReturn(course);
        assertFalse(auth.canModifyCourse(session, "CS101"));
    }

    @Test
    void canModifyCourseShouldFallbackToTeacherNameWhenNoTeacherNo() {
        Teacher teacher = new Teacher();
        teacher.setRole("teacher");
        teacher.setName("张老师");
        session.setAttribute("teacher", teacher);
        Course course = new Course();
        course.setTeacher("张老师");
        when(courseService.getById("CS101")).thenReturn(course);
        assertTrue(auth.canModifyCourse(session, "CS101"));
    }

    @Test
    void canModifyCourseShouldReturnFalseWhenCourseNotFound() {
        Teacher teacher = new Teacher();
        teacher.setRole("teacher");
        teacher.setTeacherNo("T001");
        session.setAttribute("teacher", teacher);
        when(courseService.getById("CS999")).thenReturn(null);
        assertFalse(auth.canModifyCourse(session, "CS999"));
    }
}

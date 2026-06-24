package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.service.CourseService;
import com.neu.CoursePlatform.service.FileStorageService;
import com.neu.CoursePlatform.service.TeacherService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock
    private CourseService courseService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private TeacherService teacherService;

    @Mock
    private Auth auth;

    @Mock
    private HttpSession session;

    @InjectMocks
    private CourseController courseController;

    @Test
    void teacherCreatesCourseWithExtendedInformation() {
        Teacher teacher = new Teacher();
        teacher.setName("Teacher A");
        teacher.setTeacherNo("12");
        teacher.setRole("teacher");
        when(auth.getTeacher(session)).thenReturn(teacher);
        when(auth.isAdmin(session)).thenReturn(false);

        Result<String> result = courseController.add(
                "Python Data Analysis", null, null, 4, 64,
                "Course introduction", "Software Engineering", "Learn data analysis",
                null, session);

        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseService).save(courseCaptor.capture());
        Course saved = courseCaptor.getValue();
        assertEquals(200, result.getCode());
        assertEquals("Teacher A", saved.getTeacher());
        assertEquals("12", saved.getTeacherNo());
        assertEquals("Course introduction", saved.getDescription());
        assertEquals("Software Engineering", saved.getApplicableMajor());
        assertEquals("Learn data analysis", saved.getCourseObjectives());
    }

    @Test
    void invalidCreditsAreRejectedBeforeSaving() {
        when(auth.isAdmin(session)).thenReturn(true);

        Result<String> result = courseController.add(
                "Python Data Analysis", "Teacher A", null, -1, 64,
                null, null, null, null, session);

        assertEquals(500, result.getCode());
        assertNotNull(result.getMsg());
        verify(courseService, never()).save(any(Course.class));
    }

    @Test
    void unauthorizedCourseDeletionIsRejected() {
        when(auth.canModifyCourse(session, "1")).thenReturn(false);

        Result<Void> result = courseController.delete("1", session);

        assertEquals(500, result.getCode());
        verify(courseService, never()).removeById(anyString());
    }
}

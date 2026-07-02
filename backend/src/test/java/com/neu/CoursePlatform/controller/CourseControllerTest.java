package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.CourseDTO;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.Lesson;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.service.CourseGameConfigService;
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
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock private CourseService courseService;
    @Mock private FileStorageService fileStorageService;
    @Mock private TeacherService teacherService;
    @Mock private CourseGameConfigService courseGameConfigService;
    @Mock private Auth auth;
    @Mock private HttpSession session;
    @InjectMocks private CourseController courseController;

    // ============ gameConfig ============

    @Test
    void gameConfigRequiresLogin() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<Map<String, Object>> result = courseController.gameConfig("CS101", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void gameConfigCourseNotFound() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(courseService.getById("CS101")).thenReturn(null);

        Result<Map<String, Object>> result = courseController.gameConfig("CS101", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void gameConfigReturnsConfig() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(courseService.getById("CS101")).thenReturn(new Course());
        when(courseGameConfigService.isEnabled("CS101")).thenReturn(true);

        Result<Map<String, Object>> result = courseController.gameConfig("CS101", session);

        assertEquals(200, result.getCode());
        assertEquals("CS101", result.getData().get("courseId"));
        assertEquals(true, result.getData().get("game_mode_enabled"));
    }

    // ============ updateGameConfig ============

    @Test
    void updateGameConfigRequiresPermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<Void> result = courseController.updateGameConfig("CS101", Map.of("game_mode_enabled", true), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void updateGameConfigBodyNull() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<Void> result = courseController.updateGameConfig("CS101", null, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("game_mode_enabled"));
    }

    @Test
    void updateGameConfigEnabledNull() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<Void> result = courseController.updateGameConfig("CS101", Map.of(), session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("game_mode_enabled"));
    }

    @Test
    void updateGameConfigSuccess() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(courseGameConfigService.updateEnabled("CS101", true)).thenReturn(true);

        Result<Void> result = courseController.updateGameConfig("CS101", Map.of("game_mode_enabled", true), session);

        assertEquals(200, result.getCode());
    }

    @Test
    void updateGameConfigFails() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(courseGameConfigService.updateEnabled("CS101", true)).thenReturn(false);

        Result<Void> result = courseController.updateGameConfig("CS101", Map.of("game_mode_enabled", true), session);

        assertEquals(500, result.getCode());
    }

    // ============ search ============

    @Test
    void searchRequiresLogin() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<List<CourseDTO>> result = courseController.search("keyword", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void searchReturnsResults() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(courseService.searchDtoByKeyword("Java")).thenReturn(List.of(new CourseDTO()));

        Result<List<CourseDTO>> result = courseController.search("Java", session);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
    }

    // ============ lessons ============

    @Test
    void lessonsRequiresLogin() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<List<Lesson>> result = courseController.lessons("CS101", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void lessonsReturnsList() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(courseService.listLessons("CS101")).thenReturn(List.of(new Lesson()));

        Result<List<Lesson>> result = courseController.lessons("CS101", session);

        assertEquals(200, result.getCode());
    }

    // ============ list ============

    @Test
    void listRequiresAdmin() {
        when(auth.isAdmin(session)).thenReturn(false);

        Result<List<CourseDTO>> result = courseController.list(session);

        assertEquals(500, result.getCode());
    }

    @Test
    void listReturnsAll() {
        when(auth.isAdmin(session)).thenReturn(true);
        when(courseService.listDto()).thenReturn(List.of(new CourseDTO()));

        Result<List<CourseDTO>> result = courseController.list(session);

        assertEquals(200, result.getCode());
    }

    // ============ getByCode ============

    @Test
    void getByCodeRequiresLogin() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<Course> result = courseController.getByCode("CS101", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void getByCodeReturnsCourse() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        Course c = new Course();
        c.setCourseCode("CS101");
        when(courseService.getById("CS101")).thenReturn(c);

        Result<Course> result = courseController.getByCode("CS101", session);

        assertEquals(200, result.getCode());
    }

    @Test
    void getByCodeNotFound() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(courseService.getById("CS999")).thenReturn(null);

        Result<Course> result = courseController.getByCode("CS999", session);

        assertEquals(500, result.getCode());
    }

    // ============ add ============

    @Test
    void addUnauthorizedNoTeacher() {
        when(auth.isAdmin(session)).thenReturn(false);
        when(auth.getTeacher(session)).thenReturn(null);

        Result<String> result = courseController.add("Course", null, null, 3, 48,
                null, null, null, null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void addBlankNameRejected() {
        when(auth.isAdmin(session)).thenReturn(true);

        Result<String> result = courseController.add("  ", null, null, 3, 48,
                null, null, null, null, session);

        assertEquals(500, result.getCode());
        verify(courseService, never()).save(any());
    }

    @Test
    void addNegativeCreditsRejected() {
        when(auth.isAdmin(session)).thenReturn(true);

        Result<String> result = courseController.add("Course", null, null, -1, 48,
                null, null, null, null, session);

        assertEquals(500, result.getCode());
        verify(courseService, never()).save(any());
    }

    @Test
    void addNegativeHoursRejected() {
        when(auth.isAdmin(session)).thenReturn(true);

        Result<String> result = courseController.add("Course", null, null, 3, -1,
                null, null, null, null, session);

        assertEquals(500, result.getCode());
        verify(courseService, never()).save(any());
    }

    @Test
    void addTeacherNotFound() {
        when(auth.isAdmin(session)).thenReturn(true);
        when(teacherService.getById("T999")).thenReturn(null);

        Result<String> result = courseController.add("Course", "Someone", "T999", 3, 48,
                null, null, null, null, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("授课教师不存在"));
    }

    @Test
    void addNoTeacherName() {
        when(auth.isAdmin(session)).thenReturn(true);

        Result<String> result = courseController.add("Course", null, null, 3, 48,
                null, null, null, null, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("授课教师"));
    }

    @Test
    void addAdminCreatesCourse() {
        when(auth.isAdmin(session)).thenReturn(true);
        Teacher t = new Teacher();
        t.setName("张老师");
        t.setTeacherNo("T001");
        when(teacherService.getById("T001")).thenReturn(t);

        Result<String> result = courseController.add("Java编程", "张老师", "T001", 3, 48,
                "Java入门", "计算机", "掌握Java基础", null, session);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseService).save(captor.capture());
        Course saved = captor.getValue();
        assertEquals(200, result.getCode());
        assertEquals("Java编程", saved.getCourseName());
        assertEquals("张老师", saved.getTeacher());
        assertEquals("T001", saved.getTeacherNo());
        assertEquals("计算机", saved.getApplicableMajor());
    }

    @Test
    void teacherCreatesCourse() {
        Teacher teacher = new Teacher();
        teacher.setName("Teacher A");
        teacher.setTeacherNo("12");
        when(auth.getTeacher(session)).thenReturn(teacher);
        when(auth.isAdmin(session)).thenReturn(false);

        Result<String> result = courseController.add("Python", null, null, 4, 64,
                "Course intro", "SE", "Learn", null, session);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseService).save(captor.capture());
        Course saved = captor.getValue();
        assertEquals(200, result.getCode());
        assertEquals("Teacher A", saved.getTeacher());
        assertEquals("12", saved.getTeacherNo());
    }

    @Test
    void addWithFile() throws IOException {
        when(auth.isAdmin(session)).thenReturn(true);
        Teacher t = new Teacher();
        t.setName("张老师");
        t.setTeacherNo("T001");
        when(teacherService.getById("T001")).thenReturn(t);
        when(fileStorageService.store(any(), anyString())).thenReturn("cover.jpg");
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[]{1});

        Result<String> result = courseController.add("Java", "张老师", "T001", 3, 48,
                null, null, null, file, session);

        assertEquals(200, result.getCode());
    }

    @Test
    void addFileUploadFails() throws IOException {
        when(auth.isAdmin(session)).thenReturn(true);
        Teacher t = new Teacher();
        t.setName("张老师");
        t.setTeacherNo("T001");
        when(teacherService.getById("T001")).thenReturn(t);
        when(fileStorageService.store(any(), anyString())).thenThrow(new IOException("upload error"));
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[]{1});

        Result<String> result = courseController.add("Java", "张老师", "T001", 3, 48,
                null, null, null, file, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("上传失败"));
    }

    // ============ update ============

    @Test
    void updateRequiresPermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<String> result = courseController.update("CS101", "Name", null, null, 3, 48,
                null, null, null, null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void updateCourseNotFound() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(courseService.getById("CS101")).thenReturn(null);

        Result<String> result = courseController.update("CS101", "Name", null, null, 3, 48,
                null, null, null, null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void updateBlankNameRejected() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(courseService.getById("CS101")).thenReturn(new Course());

        Result<String> result = courseController.update("CS101", "  ", null, null, 3, 48,
                null, null, null, null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void updateTeacherNotFound() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        Course c = new Course();
        c.setCourseCode("CS101");
        when(courseService.getById("CS101")).thenReturn(c);
        when(auth.isAdmin(session)).thenReturn(true);
        when(auth.getTeacher(session)).thenReturn(null);
        when(teacherService.getById("T999")).thenReturn(null);

        Result<String> result = courseController.update("CS101", "Name", "Someone", "T999", 3, 48,
                null, null, null, null, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("授课教师不存在"));
    }

    @Test
    void updateAdminSuccess() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        Course c = new Course();
        c.setCourseCode("CS101");
        c.setCourseName("Old");
        when(courseService.getById("CS101")).thenReturn(c);
        when(auth.isAdmin(session)).thenReturn(true);
        when(auth.getTeacher(session)).thenReturn(null);
        Teacher t = new Teacher();
        t.setName("新老师");
        t.setTeacherNo("T002");
        when(teacherService.getById("T002")).thenReturn(t);

        Result<String> result = courseController.update("CS101", "NewName", "新老师", "T002", 4, 64,
                "desc", "CS", "obj", null, session);

        assertEquals(200, result.getCode());
        assertEquals("NewName", c.getCourseName());
        assertEquals("新老师", c.getTeacher());
        assertEquals("T002", c.getTeacherNo());
        verify(courseService).updateById(c);
    }

    @Test
    void updateNonAdminKeepsTeacher() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        Course c = new Course();
        c.setCourseCode("CS101");
        c.setTeacher("Original");
        when(courseService.getById("CS101")).thenReturn(c);
        when(auth.isAdmin(session)).thenReturn(false);
        when(auth.getTeacher(session)).thenReturn(null);

        Result<String> result = courseController.update("CS101", "NewName", null, null, 4, 64,
                null, null, null, null, session);

        assertEquals(200, result.getCode());
        assertEquals("Original", c.getTeacher());
        verify(courseService).updateById(c);
    }

    @Test
    void updateWithFile() throws IOException {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        Course c = new Course();
        when(courseService.getById("CS101")).thenReturn(c);
        when(auth.isAdmin(session)).thenReturn(true);
        when(fileStorageService.store(any(), anyString())).thenReturn("new_cover.jpg");
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[]{1});

        Result<String> result = courseController.update("CS101", "Name", "张老师", null, 3, 48,
                null, null, null, file, session);

        assertEquals(200, result.getCode());
    }

    @Test
    void updateFileUploadFails() throws IOException {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        Course c = new Course();
        when(courseService.getById("CS101")).thenReturn(c);
        when(auth.isAdmin(session)).thenReturn(true);
        when(fileStorageService.store(any(), anyString())).thenThrow(new IOException("error"));
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[]{1});

        Result<String> result = courseController.update("CS101", "Name", "张老师", null, 3, 48,
                null, null, null, file, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("上传失败"));
    }

    // ============ delete ============

    @Test
    void deleteRequiresPermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<Void> result = courseController.delete("CS101", session);

        assertEquals(500, result.getCode());
        verify(courseService, never()).removeById(anyString());
    }

    @Test
    void deleteSuccess() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<Void> result = courseController.delete("CS101", session);

        assertEquals(200, result.getCode());
        verify(courseService).removeById("CS101");
    }
}

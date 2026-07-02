package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.service.TeacherService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherControllerTest {

    @Mock private TeacherService teacherService;
    @Mock private Auth auth;
    @Mock private HttpSession session;
    @InjectMocks private TeacherController controller;

    // ============ register ============

    @Test
    void registerSuccess() {
        Teacher teacher = new Teacher();
        teacher.setUsername("teacher1");
        when(teacherService.register(any(Teacher.class))).thenReturn(true);

        Result<Void> result = controller.register(teacher);

        assertEquals(200, result.getCode());
    }

    @Test
    void registerFails() {
        when(teacherService.register(any(Teacher.class))).thenReturn(false);

        Result<Void> result = controller.register(new Teacher());

        assertEquals(500, result.getCode());
    }

    // ============ login ============

    @Test
    void loginSuccess() {
        Teacher req = new Teacher();
        req.setUsername("teacher1");
        req.setPassword("pass");
        Teacher found = new Teacher();
        found.setTeacherNo("T001");
        found.setName("李老师");
        when(teacherService.login("teacher1", "pass")).thenReturn(found);

        Result<Teacher> result = controller.login(req, session);

        assertEquals(200, result.getCode());
        verify(session).setAttribute("teacher", found);
    }

    @Test
    void loginFails() {
        Teacher req = new Teacher();
        req.setUsername("teacher1");
        req.setPassword("wrong");
        when(teacherService.login(anyString(), anyString())).thenReturn(null);

        Result<Teacher> result = controller.login(req, session);

        assertEquals(500, result.getCode());
    }

    // ============ search ============

    @Test
    void searchRequiresLogin() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<List<Teacher>> result = controller.search("keyword", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void searchReturnsResults() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(teacherService.searchByKeyword("李")).thenReturn(List.of(new Teacher()));

        Result<List<Teacher>> result = controller.search("李", session);

        assertEquals(200, result.getCode());
    }

    // ============ list ============

    @Test
    void listRequiresAdmin() {
        when(auth.isAdmin(session)).thenReturn(false);

        Result<List<Teacher>> result = controller.list(session);

        assertEquals(500, result.getCode());
    }

    @Test
    void listReturnsAll() {
        when(auth.isAdmin(session)).thenReturn(true);
        when(teacherService.list()).thenReturn(List.of(new Teacher()));

        Result<List<Teacher>> result = controller.list(session);

        assertEquals(200, result.getCode());
    }

    // ============ getByNo ============

    @Test
    void getByNoRequiresAdmin() {
        when(auth.isAdmin(session)).thenReturn(false);

        Result<Teacher> result = controller.getByNo("T001", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void getByNoReturnsTeacher() {
        when(auth.isAdmin(session)).thenReturn(true);
        Teacher t = new Teacher();
        t.setTeacherNo("T001");
        when(teacherService.getById("T001")).thenReturn(t);

        Result<Teacher> result = controller.getByNo("T001", session);

        assertEquals(200, result.getCode());
    }

    @Test
    void getByNoNotFound() {
        when(auth.isAdmin(session)).thenReturn(true);
        when(teacherService.getById("T999")).thenReturn(null);

        Result<Teacher> result = controller.getByNo("T999", session);

        assertEquals(500, result.getCode());
    }

    // ============ update ============

    @Test
    void updateRequiresAdmin() {
        when(auth.isAdmin(session)).thenReturn(false);

        Result<Void> result = controller.update("T001", new Teacher(), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void updateSetsTeacherNoAndSaves() {
        when(auth.isAdmin(session)).thenReturn(true);
        Teacher teacher = new Teacher();
        teacher.setName("新名字");

        Result<Void> result = controller.update("T001", teacher, session);

        assertEquals(200, result.getCode());
        assertEquals("T001", teacher.getTeacherNo());
        verify(teacherService).updateById(teacher);
    }

    // ============ delete ============

    @Test
    void deleteRequiresAdmin() {
        when(auth.isAdmin(session)).thenReturn(false);

        Result<Void> result = controller.delete("T001", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void deleteRemovesTeacher() {
        when(auth.isAdmin(session)).thenReturn(true);

        Result<Void> result = controller.delete("T001", session);

        assertEquals(200, result.getCode());
        verify(teacherService).removeById("T001");
    }
}

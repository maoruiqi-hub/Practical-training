package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.service.StudentService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.ServletOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    @Mock private StudentService studentService;
    @Mock private Auth auth;
    @Mock private HttpSession session;
    @Mock private HttpServletResponse response;
    @InjectMocks private StudentController controller;

    // ============ register ============

    @Test
    void registerSuccess() {
        Student student = new Student();
        student.setUsername("testuser");
        student.setPassword("pass123");
        when(studentService.register(any(Student.class))).thenReturn(true);

        Result<Void> result = controller.register(student);

        assertEquals(200, result.getCode());
    }

    @Test
    void registerFails() {
        when(studentService.register(any(Student.class))).thenReturn(false);

        Result<Void> result = controller.register(new Student());

        assertEquals(500, result.getCode());
        assertNotNull(result.getMsg());
    }

    // ============ login ============

    @Test
    void loginSuccess() {
        Student req = new Student();
        req.setUsername("testuser");
        req.setPassword("pass123");
        Student found = new Student();
        found.setStudentNo("2024001");
        found.setName("张三");
        when(studentService.login("testuser", "pass123")).thenReturn(found);

        Result<Student> result = controller.login(req, session);

        assertEquals(200, result.getCode());
        assertEquals("张三", result.getData().getName());
        verify(session).setAttribute("student", found);
    }

    @Test
    void loginFailsWithWrongCredentials() {
        Student req = new Student();
        req.setUsername("testuser");
        req.setPassword("wrong");
        when(studentService.login("testuser", "wrong")).thenReturn(null);

        Result<Student> result = controller.login(req, session);

        assertEquals(500, result.getCode());
        assertEquals("账号或密码错误", result.getMsg());
    }

    // ============ search ============

    @Test
    void searchRequiresAdmin() {
        when(auth.isAdmin(session)).thenReturn(false);

        Result<List<Student>> result = controller.search("keyword", session);

        assertEquals(500, result.getCode());
        verify(studentService, never()).searchByKeyword(anyString());
    }

    @Test
    void searchReturnsResults() {
        when(auth.isAdmin(session)).thenReturn(true);
        Student s = new Student();
        s.setName("张三");
        when(studentService.searchByKeyword("张")).thenReturn(List.of(s));

        Result<List<Student>> result = controller.search("张", session);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
    }

    // ============ list ============

    @Test
    void listRequiresAdmin() {
        when(auth.isAdmin(session)).thenReturn(false);

        Result<List<Student>> result = controller.list(session);

        assertEquals(500, result.getCode());
    }

    @Test
    void listReturnsAll() {
        when(auth.isAdmin(session)).thenReturn(true);
        when(studentService.list()).thenReturn(List.of(new Student()));

        Result<List<Student>> result = controller.list(session);

        assertEquals(200, result.getCode());
    }

    // ============ listByContract ============

    @Test
    void listByContractAllowsTeacher() {
        when(auth.isAdmin(session)).thenReturn(false);
        when(auth.isTeacher(session)).thenReturn(true);
        when(studentService.listByClassId(null)).thenReturn(List.of());

        Result<List<Student>> result = controller.listByContract(null, session);

        assertEquals(200, result.getCode());
    }

    @Test
    void listByContractRejectsUnauthorized() {
        when(auth.isAdmin(session)).thenReturn(false);
        when(auth.isTeacher(session)).thenReturn(false);

        Result<List<Student>> result = controller.listByContract(null, session);

        assertEquals(500, result.getCode());
    }

    // ============ getByNo ============

    @Test
    void getByNoReturnsStudent() {
        when(auth.isAdmin(session)).thenReturn(true);
        Student s = new Student();
        s.setStudentNo("2024001");
        when(studentService.getById("2024001")).thenReturn(s);

        Result<Student> result = controller.getByNo("2024001", session);

        assertEquals(200, result.getCode());
        assertEquals("2024001", result.getData().getStudentNo());
    }

    @Test
    void getByNoNotFound() {
        when(auth.isAdmin(session)).thenReturn(true);
        when(studentService.getById("999")).thenReturn(null);

        Result<Student> result = controller.getByNo("999", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void getByNoRequiresAdmin() {
        when(auth.isAdmin(session)).thenReturn(false);

        Result<Student> result = controller.getByNo("2024001", session);

        assertEquals(500, result.getCode());
    }

    // ============ update ============

    @Test
    void updateRequiresAdmin() {
        when(auth.isAdmin(session)).thenReturn(false);

        Result<Void> result = controller.update("2024001", new Student(), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void updateSetsStudentNoAndSaves() {
        when(auth.isAdmin(session)).thenReturn(true);
        Student student = new Student();
        student.setName("新名字");

        Result<Void> result = controller.update("2024001", student, session);

        assertEquals(200, result.getCode());
        assertEquals("2024001", student.getStudentNo());
        verify(studentService).updateById(student);
    }

    // ============ delete ============

    @Test
    void deleteRequiresAdmin() {
        when(auth.isAdmin(session)).thenReturn(false);

        Result<Void> result = controller.delete("2024001", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void deleteRemovesStudent() {
        when(auth.isAdmin(session)).thenReturn(true);

        Result<Void> result = controller.delete("2024001", session);

        assertEquals(200, result.getCode());
        verify(studentService).removeById("2024001");
    }

    // ============ import ============

    @Test
    void importRequiresAdmin() {
        when(auth.isAdmin(session)).thenReturn(false);

        Result<Map<String, Object>> result = controller.importStudents(null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void importSuccess() throws IOException {
        when(auth.isAdmin(session)).thenReturn(true);
        when(studentService.importFromExcel(any())).thenReturn(5);

        Result<Map<String, Object>> result = controller.importStudents(null, session);

        assertEquals(200, result.getCode());
        assertEquals(5, result.getData().get("count"));
        assertTrue(result.getData().get("message").toString().contains("5"));
    }

    @Test
    void importHandlesIOException() throws IOException {
        when(auth.isAdmin(session)).thenReturn(true);
        when(studentService.importFromExcel(any())).thenThrow(new IOException("文件格式错误"));

        Result<Map<String, Object>> result = controller.importStudents(null, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("文件格式错误"));
    }

    // ============ export ============

    @Test
    void exportRequiresAdmin() throws IOException {
        when(auth.isAdmin(session)).thenReturn(false);

        controller.exportStudents(response, session);

        verify(response).setStatus(403);
        verify(studentService, never()).exportToExcel(any());
    }

    @Test
    void exportSetsHeadersAndWritesExcel() throws IOException {
        when(auth.isAdmin(session)).thenReturn(true);
        ServletOutputStream os = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(os);

        controller.exportStudents(response, session);

        verify(response).setContentType("application/vnd.ms-excel");
        verify(response).setHeader(eq("Content-Disposition"), contains("students.xls"));
        verify(studentService).exportToExcel(os);
    }
}

package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.LoginResponse;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.service.StudentService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final Auth auth;

    public StudentController(StudentService studentService, Auth auth) {
        this.studentService = studentService;
        this.auth = auth;
    }

    /** 注册 | 公开 */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody Student student) {
        if (studentService.register(student)) return Result.ok();
        return Result.fail("注册失败");
    }

    /** 登录 | 公开 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody Student req, HttpSession session) {
        Student student = studentService.login(req.getUsername(), req.getPassword());
        if (student == null) return Result.fail("账号或密码错误");
        session.setAttribute("student", sessionStudent(student));
        return Result.ok(LoginResponse.fromStudent(student));
    }

    /** 模糊搜索学生 | admin */
    @GetMapping("/search")
    public Result<java.util.List<Student>> search(@RequestParam String keyword, HttpSession session) {
        if (!auth.isAdmin(session)) return Result.fail("无权限");
        return Result.ok(studentService.searchByKeyword(keyword));
    }

    /** 全部学生列表 | admin */
    @GetMapping
    public Result<List<Student>> listByContract(@RequestParam(name = "class_id", required = false) String classId,
                                                HttpSession session) {
        if (!auth.isAdmin(session) && !auth.isTeacher(session)) return Result.fail("无权限");
        return Result.ok(studentService.listByClassId(classId));
    }

    /** 全部学生列表 | admin */
    @GetMapping("/list")
    public Result<List<Student>> list(HttpSession session) {
        if (!auth.isAdmin(session)) return Result.fail("无权限");
        return Result.ok(studentService.list());
    }

    /** 按学号查学生 | admin */
    @GetMapping("/{studentNo}")
    public Result<Student> getByNo(@PathVariable String studentNo, HttpSession session) {
        if (!auth.isAdmin(session)) return Result.fail("无权限");
        Student s = studentService.getById(studentNo);
        return s != null ? Result.ok(s) : Result.fail("学生不存在");
    }

    /** 修改学生 | admin */
    @PutMapping("/{studentNo}")
    public Result<Void> update(@PathVariable String studentNo, @RequestBody Student student, HttpSession session) {
        if (!auth.isAdmin(session)) return Result.fail("无权限");
        student.setStudentNo(studentNo);
        studentService.updateById(student);
        return Result.ok();
    }

    /** 删除学生 | admin */
    @DeleteMapping("/{studentNo}")
    public Result<Void> delete(@PathVariable String studentNo, HttpSession session) {
        if (!auth.isAdmin(session)) return Result.fail("无权限");
        studentService.removeById(studentNo);
        return Result.ok();
    }

    /** 批量导入学生 | admin */
    @PostMapping("/import")
    public Result<Map<String, Object>> importStudents(@RequestParam MultipartFile file, HttpSession session) {
        if (!auth.isAdmin(session)) return Result.fail("无权限");
        try {
            int count = studentService.importFromExcel(file);
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("count", count);
            result.put("message", "成功导入 " + count + " 名学生");
            return Result.ok(result);
        } catch (IOException e) {
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("count", 0);
            result.put("message", e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /** 导出学生 | admin */
    @GetMapping("/export")
    public void exportStudents(HttpServletResponse response, HttpSession session) throws IOException {
        if (!auth.isAdmin(session)) {
            response.setStatus(403);
            return;
        }
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=students.xls");
        studentService.exportToExcel(response.getOutputStream());
    }

    private Student sessionStudent(Student source) {
        Student student = new Student();
        student.setStudentNo(source.getStudentNo());
        student.setName(source.getName());
        student.setUsername(source.getUsername());
        student.setCollege(source.getCollege());
        student.setClassName(source.getClassName());
        return student;
    }
}

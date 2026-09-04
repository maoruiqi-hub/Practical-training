package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.LoginResponse;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.service.TeacherService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherService teacherService;
    private final Auth auth;

    public TeacherController(TeacherService teacherService, Auth auth) {
        this.teacherService = teacherService;
        this.auth = auth;
    }

    /** 注册 | 公开 */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody Teacher teacher) {
        if (teacherService.register(teacher)) return Result.ok();
        return Result.fail("注册失败");
    }

    /** 登录 | 公开 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody Teacher req, HttpSession session) {
        Teacher teacher = teacherService.login(req.getUsername(), req.getPassword());
        if (teacher == null) return Result.fail("账号或密码错误");
        session.setAttribute("teacher", sessionTeacher(teacher));
        return Result.ok(LoginResponse.fromTeacher(teacher));
    }

    /** 模糊搜索教师 | 登录用户 */
    @GetMapping("/search")
    public Result<List<Teacher>> search(@RequestParam String keyword, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(teacherService.searchByKeyword(keyword));
    }

    /** 全部教师列表 | admin */
    @GetMapping("/list")
    public Result<List<Teacher>> list(HttpSession session) {
        if (!auth.isAdmin(session)) return Result.fail("无权限");
        return Result.ok(teacherService.list());
    }

    /** 按教职工码查教师 | admin */
    @GetMapping("/{teacherNo}")
    public Result<Teacher> getByNo(@PathVariable String teacherNo, HttpSession session) {
        if (!auth.isAdmin(session)) return Result.fail("无权限");
        Teacher t = teacherService.getById(teacherNo);
        return t != null ? Result.ok(t) : Result.fail("教师不存在");
    }

    /** 修改教师 | admin */
    @PutMapping("/{teacherNo}")
    public Result<Void> update(@PathVariable String teacherNo, @RequestBody Teacher teacher, HttpSession session) {
        if (!auth.isAdmin(session)) return Result.fail("无权限");
        teacher.setTeacherNo(teacherNo);
        teacherService.updateById(teacher);
        return Result.ok();
    }

    /** 删除教师 | admin */
    @DeleteMapping("/{teacherNo}")
    public Result<Void> delete(@PathVariable String teacherNo, HttpSession session) {
        if (!auth.isAdmin(session)) return Result.fail("无权限");
        teacherService.removeById(teacherNo);
        return Result.ok();
    }

    private Teacher sessionTeacher(Teacher source) {
        Teacher teacher = new Teacher();
        teacher.setTeacherNo(source.getTeacherNo());
        teacher.setName(source.getName());
        teacher.setUsername(source.getUsername());
        teacher.setCollege(source.getCollege());
        teacher.setMajor(source.getMajor());
        teacher.setRole(source.getRole());
        return teacher;
    }
}

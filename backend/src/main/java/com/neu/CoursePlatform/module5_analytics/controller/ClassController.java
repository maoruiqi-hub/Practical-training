package com.neu.CoursePlatform.module5_analytics.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.module5_analytics.entity.ClassInfo;
import com.neu.CoursePlatform.module5_analytics.service.ClassInfoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 班级管理 Controller（R1 组需求）
 */
@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassInfoService classInfoService;
    private final Auth auth;

    public ClassController(ClassInfoService classInfoService, Auth auth) {
        this.classInfoService = classInfoService;
        this.auth = auth;
    }

    /** R1.1 创建班级 | 教师 */
    @PostMapping
    public Result<ClassInfo> create(@RequestBody ClassInfo classInfo, HttpSession session) {
        Teacher teacher = auth.getTeacher(session);
        if (teacher == null) return Result.fail("请先登录");
        classInfo.setTeacherId(auth.getTeacherId(session));
        ClassInfo created = classInfoService.createClass(classInfo);
        if (created == null) return Result.fail("该课程下班级名称已存在");
        return Result.ok(created);
    }

    /** R1.2 修改班级 | 教师 */
    @PutMapping("/{id}")
    public Result<ClassInfo> update(@PathVariable String id,
                                     @RequestBody ClassInfo classInfo,
                                     HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        ClassInfo updated = classInfoService.updateClass(id, classInfo);
        if (updated == null) return Result.fail("班级不存在或名称重复");
        return Result.ok(updated);
    }

    /** R1.3 教师班级列表 */
    @GetMapping
    public Result<List<ClassInfo>> list(@RequestParam(required = false) String teacherId,
                                         HttpSession session) {
        Teacher teacher = auth.getTeacher(session);
        if (teacher == null) return Result.fail("请先登录");
        String tid = teacherId != null ? teacherId : auth.getTeacherId(session);
        return Result.ok(classInfoService.listByTeacher(tid));
    }

    /** R1.4 班级详情（含学生列表） */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable String id, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        ClassInfo classInfo = classInfoService.getById(id);
        if (classInfo == null) return Result.fail("班级不存在");
        List<String> studentIds = classInfoService.getStudentIds(id);
        return Result.ok(Map.of("classInfo", classInfo, "studentIds", studentIds));
    }

    /** R1.5 添加学生到班级 | 教师 */
    @PostMapping("/{id}/enroll")
    public Result<Void> enroll(@PathVariable String id,
                                @RequestBody Map<String, String> body,
                                HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        String studentId = body.get("studentId");
        if (studentId == null || studentId.isEmpty()) return Result.fail("studentId 不能为空");
        boolean ok = classInfoService.enrollStudent(id, studentId);
        return ok ? Result.ok() : Result.fail("添加失败（学生可能已在班级中）");
    }

    /** R1.6 从班级移除学生 | 教师 */
    @DeleteMapping("/{id}/students/{sid}")
    public Result<Void> removeStudent(@PathVariable String id,
                                       @PathVariable String sid,
                                       HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        classInfoService.removeStudent(id, sid);
        return Result.ok();
    }

    /** R1.7 删除班级（无学生时） | 教师 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id, HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        boolean ok = classInfoService.deleteClassIfEmpty(id);
        if (!ok) return Result.fail("班级不存在或仍有学生，请先将学生移除或转移");
        return Result.ok();
    }
}

package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.service.StatsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatsController {

    private final StatsService statsService;
    private final Auth auth;

    public StatsController(StatsService statsService, Auth auth) {
        this.statsService = statsService;
        this.auth = auth;
    }

    /** 学生成绩总览 | student本人 / admin */
    @GetMapping("/students/{studentNo}/stats")
    public Result<Map<String, Object>> studentStats(@PathVariable String studentNo, HttpSession session) {
        Student loginStudent = (Student) session.getAttribute("student");
        if (!auth.isAdmin(session) && (loginStudent == null || !loginStudent.getStudentNo().equals(studentNo))) {
            return Result.fail("无权限");
        }
        return Result.ok(statsService.buildStudentStats(studentNo));
    }

    /** 学生在某课程中的统计 | student本人 / admin */
    @GetMapping("/student/{studentNo}/course/{courseCode}")
    public Result<Map<String, Object>> studentCourseStats(@PathVariable String studentNo,
                                                           @PathVariable String courseCode,
                                                           HttpSession session) {
        Student loginStudent = (Student) session.getAttribute("student");
        if (!auth.isAdmin(session) && (loginStudent == null || !loginStudent.getStudentNo().equals(studentNo))) {
            return Result.fail("无权限");
        }
        return Result.ok(statsService.buildStudentCourseStats(studentNo, courseCode));
    }

    /** 课程成绩总览 | admin/授课教师 */
    @GetMapping("/courses/{courseCode}/stats")
    public Result<Map<String, Object>> courseStats(@PathVariable String courseCode, HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        return Result.ok(statsService.buildCourseStats(courseCode));
    }
}

package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.service.ProgressService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class ProgressController {

    private final ProgressService progressService;
    private final Auth auth;

    public ProgressController(ProgressService progressService, Auth auth) {
        this.progressService = progressService;
        this.auth = auth;
    }

    /** 学生查看自己某课程的学习进度 | student本人
     *  GET /api/students/{studentNo}/progress?courseCode=  */
    @GetMapping({"/api/students/{studentNo}/progress", "/progress/student/{studentNo}"})
    public Result<Map<String, Object>> studentProgress(@PathVariable String studentNo,
                                                        @RequestParam String courseCode,
                                                        HttpSession session) {
        Student loginStudent = (Student) session.getAttribute("student");
        if (!auth.isAdmin(session) && (loginStudent == null || !loginStudent.getStudentNo().equals(studentNo))) {
            return Result.fail("无权限");
        }
        return Result.ok(progressService.buildStudentProgress(studentNo, courseCode));
    }

    /** 教师查看某课程的全班学习进度 | admin/授课教师
     *  GET /api/courses/{courseCode}/progress */
    @GetMapping({"/api/courses/{courseCode}/progress", "/progress/course/{courseCode}"})
    public Result<Map<String, Object>> courseProgress(@PathVariable String courseCode, HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        return Result.ok(progressService.buildCourseProgress(courseCode));
    }
}

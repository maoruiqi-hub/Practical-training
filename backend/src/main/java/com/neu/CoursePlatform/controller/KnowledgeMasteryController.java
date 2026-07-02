package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.KnowledgeMasteryUpdateRequest;
import com.neu.CoursePlatform.entity.KnowledgeMastery;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.service.KnowledgeMasteryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Student mastery is personal data. Students may read only their own data;
 * teachers may view or manually correct data only for courses they own.
 */
@RestController
@RequestMapping("/api/knowledge-mastery")
public class KnowledgeMasteryController {

    private final KnowledgeMasteryService masteryService;
    private final Auth auth;

    public KnowledgeMasteryController(KnowledgeMasteryService masteryService, Auth auth) {
        this.masteryService = masteryService;
        this.auth = auth;
    }

    @GetMapping("/student/{studentNo}")
    public Result<List<KnowledgeMastery>> list(@PathVariable String studentNo,
                                                @RequestParam String courseCode,
                                                HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Student student = auth.getStudent(session);
        if (student != null && !studentNo.equals(student.getStudentNo())) {
            return Result.fail("学生只能查看自己的知识点掌握度");
        }
        if (student == null && !auth.canModifyCourse(session, courseCode)) {
            return Result.fail("无权查看该课程的掌握度数据");
        }
        return Result.ok(masteryService.listByStudentAndCourse(studentNo, courseCode));
    }

    /**
     * This endpoint is for a course teacher's manual correction. Task, quiz,
     * report and learning-behaviour modules should call the service directly
     * after computing their evidence rather than exposing a student-writable API.
     */
    @PostMapping
    public Result<KnowledgeMastery> upsert(@RequestBody KnowledgeMasteryUpdateRequest request,
                                           HttpSession session) {
        if (request == null || !auth.canModifyCourse(session, request.getCourseCode())) {
            return Result.fail("无权维护该课程的掌握度数据");
        }
        String validationMessage = masteryService.validateForUpsert(request);
        if (validationMessage != null) return Result.fail(validationMessage);
        return Result.ok(masteryService.upsert(request));
    }
}

package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.WeakKnowledgePointDTO;
import com.neu.CoursePlatform.service.CourseService;
import com.neu.CoursePlatform.service.WeakPointService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class WeakPointController {

    private final WeakPointService weakPointService;
    private final CourseService courseService;
    private final Auth auth;

    public WeakPointController(WeakPointService weakPointService,
                               CourseService courseService,
                               Auth auth) {
        this.weakPointService = weakPointService;
        this.courseService = courseService;
        this.auth = auth;
    }

    @GetMapping("/{courseCode}/weak-points")
    public Result<List<WeakKnowledgePointDTO>> list(@PathVariable String courseCode,
                                                     HttpSession session) {
        if (courseService.getById(courseCode) == null) return Result.fail("课程不存在");
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权查看班级薄弱知识点统计");
        return Result.ok(weakPointService.listByCourseCode(courseCode));
    }
}

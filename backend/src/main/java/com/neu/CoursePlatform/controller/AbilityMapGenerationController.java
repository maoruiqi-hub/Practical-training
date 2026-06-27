package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.service.CourseAiService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ability-map")
public class AbilityMapGenerationController {
    private final CourseAiService courseAiService;
    private final Auth auth;

    public AbilityMapGenerationController(CourseAiService courseAiService, Auth auth) {
        this.courseAiService = courseAiService;
        this.auth = auth;
    }

    @PostMapping("/generate")
    public Result<AgenticResponse> generate(@RequestParam String courseCode, HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        return courseAiService.generateAbilityMap(courseCode);
    }
}

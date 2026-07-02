package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.CourseQaRequest;
import com.neu.CoursePlatform.service.CourseAiService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge-points")
public class CourseQaController {
    private final CourseAiService courseAiService;
    private final Auth auth;

    public CourseQaController(CourseAiService courseAiService, Auth auth) {
        this.courseAiService = courseAiService;
        this.auth = auth;
    }

    @PostMapping("/{knowledgePointId}/qa")
    public Result<AgenticResponse> ask(@PathVariable String knowledgePointId,
                                       @RequestBody CourseQaRequest request,
                                       HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return courseAiService.answerKnowledgePointQuestion(knowledgePointId, request);
    }
}

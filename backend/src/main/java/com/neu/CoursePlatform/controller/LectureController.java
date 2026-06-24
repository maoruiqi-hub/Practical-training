package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.agentic.AgenticRequest;
import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.LectureRequest;
import com.neu.CoursePlatform.entity.CourseResource;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.service.CourseResourceService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge-points")
public class LectureController {

    private final AgenticClient agenticClient;
    private final KnowledgePointService knowledgePointService;
    private final CourseResourceService courseResourceService;
    private final Auth auth;

    public LectureController(AgenticClient agenticClient, KnowledgePointService knowledgePointService,
                             CourseResourceService courseResourceService, Auth auth) {
        this.agenticClient = agenticClient;
        this.knowledgePointService = knowledgePointService;
        this.courseResourceService = courseResourceService;
        this.auth = auth;
    }

    @PostMapping({"/{knowledgePointId}/lecture", "/{knowledgePointId}/explain"})
    public Result<AgenticResponse> lecture(@PathVariable String knowledgePointId,
                                            @RequestBody LectureRequest request,
                                            HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        KnowledgePoint point = knowledgePointService.getById(knowledgePointId);
        if (point == null) return Result.fail("知识点不存在");
        if (request == null || request.getResourceId() == null || request.getResourceId().isBlank()) {
            return Result.fail("请选择 PPT 资源");
        }
        CourseResource resource = courseResourceService.getById(request.getResourceId());
        if (resource == null || !point.getCourseCode().equals(resource.getCourseCode())
                || !"ppt".equals(resource.getResourceType())) {
            return Result.fail("PPT 资源不属于当前课程");
        }
        AgenticRequest input = new AgenticRequest();
        input.setCourseCode(point.getCourseCode());
        input.setResourceId(resource.getResourceId());
        input.setKnowledgePointId(knowledgePointId);
        input.setContent(request.getQuestion());
        input.setContext(Map.of("pageNumber", request.getPageNumber() == null ? 0 : request.getPageNumber(),
                "previousMessages", request.getPreviousMessages() == null ? List.of() : request.getPreviousMessages(),
                "knowledgePoint", point.getName()));
        AgenticResponse response = agenticClient.invoke("lecture", input);
        return response.isSuccess() ? Result.ok(response) : Result.fail(response.getMessage());
    }
}

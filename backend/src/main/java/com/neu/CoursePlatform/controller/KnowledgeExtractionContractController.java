package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.KnowledgeExtractionCandidate;
import com.neu.CoursePlatform.service.KnowledgeExtractionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseCode}/knowledge-points/extract")
public class KnowledgeExtractionContractController {

    private final KnowledgeExtractionService extractionService;
    private final Auth auth;

    public KnowledgeExtractionContractController(KnowledgeExtractionService extractionService, Auth auth) {
        this.extractionService = extractionService;
        this.auth = auth;
    }

    @PostMapping
    public Result<List<KnowledgeExtractionCandidate>> extract(@PathVariable String courseCode,
                                                              @RequestParam String resourceId,
                                                              HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权提取该课程的知识点");
        try {
            return Result.ok(extractionService.extract(courseCode, resourceId));
        } catch (IllegalArgumentException exception) {
            return Result.fail(exception.getMessage());
        }
    }
}

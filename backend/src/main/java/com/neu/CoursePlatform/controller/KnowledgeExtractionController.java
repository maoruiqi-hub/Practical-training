package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.KnowledgeExtractionCandidate;
import com.neu.CoursePlatform.service.KnowledgeExtractionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseCode}/knowledge-extraction")
public class KnowledgeExtractionController {

    private final KnowledgeExtractionService extractionService;
    private final Auth auth;

    public KnowledgeExtractionController(KnowledgeExtractionService extractionService, Auth auth) {
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

    @GetMapping
    public Result<List<KnowledgeExtractionCandidate>> list(@PathVariable String courseCode,
                                                            HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权查看该课程的提取结果");
        return Result.ok(extractionService.listPending(courseCode));
    }

    @PostMapping("/{candidateId}/accept")
    public Result<Void> accept(@PathVariable String courseCode,
                               @PathVariable String candidateId,
                               @RequestBody KnowledgeExtractionCandidate editedCandidate,
                               HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权审核该课程的知识点");
        try {
            String validationMessage = extractionService.accept(courseCode, candidateId, editedCandidate);
            return validationMessage == null ? Result.ok() : Result.fail(validationMessage);
        } catch (IllegalArgumentException exception) {
            return Result.fail(exception.getMessage());
        }
    }

    @PostMapping("/{candidateId}/reject")
    public Result<Void> reject(@PathVariable String courseCode,
                               @PathVariable String candidateId,
                               HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权审核该课程的知识点");
        try {
            extractionService.reject(courseCode, candidateId);
            return Result.ok();
        } catch (IllegalArgumentException exception) {
            return Result.fail(exception.getMessage());
        }
    }
}

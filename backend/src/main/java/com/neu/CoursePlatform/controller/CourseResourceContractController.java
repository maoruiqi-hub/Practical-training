package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Result;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/courses/{courseCode}/resources")
public class CourseResourceContractController {

    private final CourseResourceController delegate;

    public CourseResourceContractController(CourseResourceController delegate) {
        this.delegate = delegate;
    }

    @GetMapping
    public Result<?> list(@PathVariable String courseCode,
                          @RequestParam(required = false) String chapter,
                          @RequestParam(required = false) String knowledgePointId,
                          @RequestParam(required = false) String resourceType,
                          HttpSession session) {
        return delegate.list(courseCode, chapter, knowledgePointId, resourceType, session);
    }

    @PostMapping
    public Result<String> upload(@PathVariable String courseCode,
                                 @RequestParam(required = false) String title,
                                 @RequestParam(required = false) String chapter,
                                 @RequestParam(required = false) String knowledgePointId,
                                 @RequestParam(required = false) String resourceType,
                                 @RequestParam MultipartFile file,
                                 HttpSession session) {
        return delegate.upload(courseCode, title, chapter, knowledgePointId, resourceType, file, session);
    }
}

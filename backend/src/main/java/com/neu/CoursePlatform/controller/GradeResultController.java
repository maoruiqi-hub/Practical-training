package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.GradeResult;
import com.neu.CoursePlatform.entity.SubmissionAiReview;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.service.GradeResultService;
import com.neu.CoursePlatform.service.SubmissionAiReviewService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class GradeResultController {

    private final TaskSubmissionService submissionService;
    private final SubmissionAiReviewService aiReviewService;
    private final GradeResultService gradeResultService;
    private final Auth auth;

    public GradeResultController(TaskSubmissionService submissionService,
                                 SubmissionAiReviewService aiReviewService,
                                 GradeResultService gradeResultService,
                                 Auth auth) {
        this.submissionService = submissionService;
        this.aiReviewService = aiReviewService;
        this.gradeResultService = gradeResultService;
        this.auth = auth;
    }

    @PostMapping("/submissions/{submissionId}/grade")
    public Result<GradeResult> generateAiGrade(@PathVariable String submissionId, HttpSession session) {
        TaskSubmission submission = submissionService.getById(submissionId);
        if (submission == null) return Result.fail("提交记录不存在");
        String courseCode = submissionService.getTaskCourseCode(submission.getTaskNo());
        if (courseCode == null || !auth.canModifyCourse(session, courseCode)) return Result.fail("无权评分");
        try {
            SubmissionAiReview review = aiReviewService.generateReview(submissionId);
            GradeResult result = new GradeResult();
            result.setTargetId(submissionId);
            result.setTargetType("submission");
            result.setCourseCode(courseCode);
            result.setStudentNo(submission.getStudentNo());
            result.setScore(review.getAiScore());
            result.setFeedback(review.getSummary());
            result.setGradedBy("ai");
            result.setCreateTime(LocalDateTime.now());
            result.setUpdatedAt(LocalDateTime.now());
            gradeResultService.save(result);
            return Result.ok(result);
        } catch (IllegalArgumentException exception) {
            return Result.fail(exception.getMessage());
        }
    }

    @PutMapping("/grade-results/{gradeResultId}")
    public Result<GradeResult> confirmGrade(@PathVariable String gradeResultId,
                                            @RequestBody GradeResult request,
                                            HttpSession session) {
        GradeResult result = gradeResultService.getById(gradeResultId);
        if (result == null) return Result.fail("评分结果不存在");
        if (!auth.canModifyCourse(session, result.getCourseCode())) return Result.fail("无权调整评分");
        if (request == null || request.getScore() == null || request.getScore() < 0) return Result.fail("评分不合法");
        result.setScore(request.getScore());
        result.setFeedback(request.getFeedback());
        result.setGradedBy("manual");
        result.setUpdatedAt(LocalDateTime.now());
        gradeResultService.updateById(result);
        TaskSubmission submission = submissionService.getById(result.getTargetId());
        if (submission != null) {
            submission.setScore(result.getScore());
            submission.setFeedback(result.getFeedback());
            submission.setStatus("graded");
            submissionService.updateById(submission);
        }
        return Result.ok(result);
    }
}

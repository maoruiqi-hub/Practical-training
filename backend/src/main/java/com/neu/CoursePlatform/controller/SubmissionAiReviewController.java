package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.SubmissionAiReview;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.SubmissionAiReviewService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubmissionAiReviewController {

    private final SubmissionAiReviewService aiReviewService;
    private final TaskSubmissionService submissionService;
    private final LearningTaskService taskService;
    private final Auth auth;

    public SubmissionAiReviewController(SubmissionAiReviewService aiReviewService,
                                         TaskSubmissionService submissionService,
                                         LearningTaskService taskService,
                                         Auth auth) {
        this.aiReviewService = aiReviewService;
        this.submissionService = submissionService;
        this.taskService = taskService;
        this.auth = auth;
    }

    /** 触发 AI 辅助评价 | 学生本人或授课教师 */
    @PostMapping("/submission/ai-review/{submissionId}")
    public Result<SubmissionAiReview> generate(@PathVariable String submissionId, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");

        TaskSubmission submission = submissionService.getById(submissionId);
        if (submission == null) return Result.fail("提交记录不存在");

        String error = checkPermission(session, submission);
        if (error != null) return Result.fail(error);

        try {
            return Result.ok(aiReviewService.generateReview(submissionId));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("AI 评阅生成失败，请稍后重试");
        }
    }

    /** 获取已有的 AI 评价结果 | 学生本人或授课教师 */
    @GetMapping("/submission/ai-review/{submissionId}")
    public Result<SubmissionAiReview> get(@PathVariable String submissionId, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");

        TaskSubmission submission = submissionService.getById(submissionId);
        if (submission == null) return Result.fail("提交记录不存在");

        String error = checkPermission(session, submission);
        if (error != null) return Result.fail(error);

        SubmissionAiReview review = aiReviewService.getLatestBySubmissionId(submissionId);
        return Result.ok(review);
    }

    private String checkPermission(HttpSession session, TaskSubmission submission) {
        Student student = auth.getStudent(session);
        if (student != null) {
            if (!student.getStudentNo().equals(submission.getStudentNo())) {
                return "无权查看他人的评阅";
            }
            return null;
        }

        Teacher teacher = auth.getTeacher(session);
        if (teacher != null) {
            LearningTask task = taskService.getById(submission.getTaskNo());
            if (task == null || !auth.canModifyCourse(session, task.getCourseCode())) {
                return "无权评阅该课程的任务";
            }
            return null;
        }

        return "请先登录";
    }
}

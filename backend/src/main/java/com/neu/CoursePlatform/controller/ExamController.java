package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.ExamGenerateRequest;
import com.neu.CoursePlatform.dto.ExamGenerateResult;
import com.neu.CoursePlatform.dto.ExamDetailDTO;
import com.neu.CoursePlatform.dto.ExamSubmitRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.entity.Exam;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.service.ExamService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.QuestionService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final QuestionService questionService;
    private final ExamService examService;
    private final LearningTaskService taskService;
    private final TaskSubmissionService submissionService;
    private final Auth auth;

    public ExamController(QuestionService questionService, ExamService examService,
                          LearningTaskService taskService, TaskSubmissionService submissionService, Auth auth) {
        this.questionService = questionService;
        this.examService = examService;
        this.taskService = taskService;
        this.submissionService = submissionService;
        this.auth = auth;
    }

    @GetMapping
    public Result<List<Exam>> list(@RequestParam(name = "course_id", required = false) String courseId,
                                   @RequestParam(name = "courseCode", required = false) String courseCode,
                                   HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        String resolvedCourseCode = courseId == null || courseId.isBlank() ? courseCode : courseId;
        if (resolvedCourseCode == null || resolvedCourseCode.isBlank()) return Result.fail("course_id 不能为空");
        return Result.ok(examService.listByCourseCode(resolvedCourseCode));
    }

    @GetMapping("/{examId}")
    public Result<ExamDetailDTO> detail(@PathVariable String examId, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        ExamDetailDTO detail = examService.getDetail(examId);
        return detail == null ? Result.fail("试卷不存在") : Result.ok(detail);
    }

    @PostMapping("/{examId}/submit")
    public Result<String> submit(@PathVariable String examId, @RequestBody ExamSubmitRequest request,
                                 HttpSession session) {
        Student student = auth.getStudent(session);
        if (student == null) return Result.fail("请使用学生账号提交试卷");
        Exam exam = examService.getById(examId);
        if (exam == null || exam.getTaskNo() == null || !"published".equals(exam.getStatus())) {
            return Result.fail("试卷不存在或尚未发布");
        }
        if (request == null || request.getAnswers() == null || request.getAnswers().isEmpty()) {
            return Result.fail("试卷答案不能为空");
        }
        if (submissionService.isTaskOverdue(exam.getTaskNo())) return Result.fail("已超过截止时间");
        if (submissionService.hasSubmitted(exam.getTaskNo(), student.getStudentNo())) return Result.fail("该试卷已提交");
        try {
            TaskSubmission submission = new TaskSubmission();
            submission.setTaskNo(exam.getTaskNo());
            submission.setStudentNo(student.getStudentNo());
            submission.setContent(new ObjectMapper().writeValueAsString(request.getAnswers().stream().map(item ->
                    java.util.Map.of("no", item.getQuestionId(), "response", item.getAnswerContent() == null ? "" : item.getAnswerContent())).toList()));
            submission.setSubmitTime(java.time.LocalDateTime.now());
            submissionService.submitWithGrading(submission);
            return Result.ok(submission.getSubmissionId());
        } catch (Exception exception) {
            return Result.fail("试卷提交失败：" + exception.getMessage());
        }
    }

    /** 按策略生成测验试卷 | admin/授课教师 */
    @PostMapping("/generate")
    public Result<List<Question>> generateExam(@RequestParam String courseCode,
                                               @RequestBody ExamGenerateRequest request,
                                               HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        try {
            return Result.ok(questionService.generateExam(courseCode, request));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 按策略生成并保存试卷版本 | admin/授课教师 */
    @PostMapping
    public Result<ExamGenerateResult> generateAndSaveExam(@RequestParam String courseCode,
                                                          @RequestBody ExamGenerateRequest request,
                                                          HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        try {
            return Result.ok(examService.generateAndSave(courseCode, request));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 将试卷版本绑定到已发布测验 | admin/授课教师 */
    @PutMapping("/{examId}/tasks/{taskNo}")
    public Result<Void> bindExamToTask(@PathVariable String examId,
                                       @PathVariable String taskNo,
                                       HttpSession session) {
        LearningTask task = taskService.getById(taskNo);
        if (task == null) return Result.fail("任务不存在");
        if (!auth.canModifyCourse(session, task.getCourseCode())) return Result.fail("无权限");
        try {
            examService.bindToTask(examId, taskNo);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
        return Result.ok();
    }
}

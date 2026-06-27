package com.neu.CoursePlatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.ExamGenerateRequest;
import com.neu.CoursePlatform.dto.ExamGenerateResult;
import com.neu.CoursePlatform.entity.Exam;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.service.ExamService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.QuestionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final QuestionService questionService;
    private final ExamService examService;
    private final LearningTaskService taskService;
    private final Auth auth;

    public ExamController(QuestionService questionService, ExamService examService,
                          LearningTaskService taskService, Auth auth) {
        this.questionService = questionService;
        this.examService = examService;
        this.taskService = taskService;
        this.auth = auth;
    }

    @GetMapping
    public Result<List<Exam>> list(@RequestParam(required = false) String courseCode,
                                   @RequestParam(name = "course_id", required = false) String courseId,
                                   HttpSession session) {
        String resolvedCourseCode = firstNonBlank(courseCode, courseId);
        if (resolvedCourseCode != null && !resolvedCourseCode.isBlank()) {
            if (!auth.canModifyCourse(session, resolvedCourseCode)) return Result.fail("无权限");
        } else if (!auth.isLoggedIn(session)) {
            return Result.fail("请先登录");
        }
        QueryWrapper<Exam> query = new QueryWrapper<>();
        if (resolvedCourseCode != null && !resolvedCourseCode.isBlank()) {
            query.eq("course_code", resolvedCourseCode);
        }
        query.orderByDesc("create_time");
        return Result.ok(examService.list(query));
    }

    @GetMapping("/{examId}")
    public Result<Exam> detail(@PathVariable String examId, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Exam exam = examService.getById(examId);
        if (exam == null) return Result.fail("试卷不存在");
        if (!auth.canModifyCourse(session, exam.getCourseCode())) return Result.fail("无权限");
        return Result.ok(exam);
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
    @RequestMapping(value = "/{examId}/tasks/{taskNo}", method = {RequestMethod.PUT, RequestMethod.POST})
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}

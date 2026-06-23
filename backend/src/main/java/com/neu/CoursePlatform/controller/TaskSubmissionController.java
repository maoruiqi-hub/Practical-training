package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.TaskSubmissionDTO;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.service.FileStorageService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/submission")
public class TaskSubmissionController {

    private final TaskSubmissionService submissionService;
    private final FileStorageService fileStorageService;
    private final Auth auth;

    public TaskSubmissionController(TaskSubmissionService submissionService, FileStorageService fileStorageService, Auth auth) {
        this.submissionService = submissionService;
        this.fileStorageService = fileStorageService;
        this.auth = auth;
    }

    /** 提交任务（文字+附件） | student */
    @PostMapping
    public Result<String> submit(@RequestParam String taskNo,
                                 @RequestParam(required = false) String content,
                                 @RequestParam(required = false) MultipartFile file,
                                 HttpSession session) {
        Student student = (Student) session.getAttribute("student");
        if (student == null) return Result.fail("请先登录学生账号");
        if (content == null && file == null) return Result.fail("提交内容不能为空");

        String courseCode = submissionService.getTaskCourseCode(taskNo);
        if (courseCode == null) return Result.fail("任务不存在");
        if (submissionService.isTaskOverdue(taskNo)) return Result.fail("已超过截止时间，无法提交");
        if (submissionService.hasSubmitted(taskNo, student.getStudentNo())) return Result.fail("已提交过，请勿重复提交");

        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo(taskNo);
        sub.setStudentNo(student.getStudentNo());
        sub.setContent(content);
        sub.setSubmitTime(LocalDateTime.now());

        if (file != null && !file.isEmpty()) {
            try {
                sub.setFilePath(fileStorageService.store(file, "../resource/HomeworkUpload/"));
            } catch (IOException e) {
                return Result.fail("文件上传失败");
            }
        }

        submissionService.applyInitialGrading(sub);
        submissionService.save(sub);
        return Result.ok("提交成功");
    }

    /** 查看某任务的所有提交（含学生名、任务类型） | admin/授课教师 */
    @GetMapping("/task/{taskNo}")
    public Result<List<TaskSubmissionDTO>> listByTask(@PathVariable String taskNo, HttpSession session) {
        String code = submissionService.getTaskCourseCode(taskNo);
        if (code == null) return Result.fail("任务不存在");
        if (!auth.canModifyCourse(session, code)) return Result.fail("无权限");
        return Result.ok(submissionService.listDtoByTaskNo(taskNo));
    }

    /** 查看我的提交 | student */
    @GetMapping("/my")
    public Result<List<TaskSubmission>> listMy(HttpSession session) {
        Student student = (Student) session.getAttribute("student");
        if (student == null) return Result.fail("请先登录学生账号");
        return Result.ok(submissionService.listByStudentNo(student.getStudentNo()));
    }

    /** 批改详情——含题目、学生答案、正确答案 | admin/授课教师 */
    @GetMapping("/grade/{submissionId}")
    public Result<Map<String, Object>> gradeDetail(@PathVariable String submissionId, HttpSession session) {
        TaskSubmission sub = submissionService.getById(submissionId);
        if (sub == null) return Result.fail("提交记录不存在");
        if (!auth.canModifyCourse(session, submissionService.getTaskCourseCode(sub.getTaskNo())))
            return Result.fail("无权限");
        return Result.ok(submissionService.buildGradeDetail(submissionId));
    }

    /** 批改打分 | admin/授课教师 */
    @PutMapping("/{submissionId}")
    public Result<Void> grade(@PathVariable String submissionId,
                              @RequestBody TaskSubmission body, HttpSession session) {
        TaskSubmission sub = submissionService.getById(submissionId);
        if (sub == null) return Result.fail("提交记录不存在");
        String code = submissionService.getTaskCourseCode(sub.getTaskNo());
        if (code == null || !auth.canModifyCourse(session, code))
            return Result.fail("无权限");

        if (body.getScore() != null) {
            sub.setScore(body.getScore());
        } else {
            sub.setScore(submissionService.autoScoreChoices(sub));
        }
        sub.setFeedback(body.getFeedback());
        sub.setStatus("graded");
        submissionService.updateById(sub);
        return Result.ok();
    }
}

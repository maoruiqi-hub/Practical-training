package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.TaskSubmissionDTO;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.service.FileStorageService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class TaskSubmissionController {

    private final TaskSubmissionService submissionService;
    private final LearningTaskService taskService;
    private final FileStorageService fileStorageService;
    private final Auth auth;

    public TaskSubmissionController(TaskSubmissionService submissionService, LearningTaskService taskService,
                                     FileStorageService fileStorageService, Auth auth) {
        this.submissionService = submissionService;
        this.taskService = taskService;
        this.fileStorageService = fileStorageService;
        this.auth = auth;
    }

    /** 提交任务（文字+附件）| student
     *  新路由: POST /api/tasks/{taskNo}/submit  旧路由: POST /submission */
    @PostMapping({"/api/tasks/{taskNo}/submit", "/submission"})
    public Result<String> submit(@PathVariable(required = false) String taskNo,
                                 @RequestParam(required = false) String taskNoParam,
                                 @RequestParam(required = false) String content,
                                 @RequestParam(required = false) MultipartFile file,
                                 HttpSession session) {
        String resolvedTaskNo = taskNo != null ? taskNo : taskNoParam;
        if (resolvedTaskNo == null) return Result.fail("缺少任务编号");
        Student student = (Student) session.getAttribute("student");
        if (student == null) return Result.fail("请先登录学生账号");

        LearningTask task = taskService.getById(resolvedTaskNo);
        if (task == null) return Result.fail("任务不存在");

        // 检查任务状态
        if ("draft".equals(task.getStatus())) return Result.fail("任务尚未发布");
        if ("closed".equals(task.getStatus())) return Result.fail("任务已关闭，不可提交");

        boolean isOverdue = task.getDeadline() != null && LocalDateTime.now().isAfter(task.getDeadline());
        if (isOverdue && (task.getAllowLate() == null || task.getAllowLate() == 0)) {
            return Result.fail("已超过截止时间，不允许逾期提交");
        }

        // 检查提交次数限制（默认3次）
        int existingCount = submissionService.countByStudentAndTask(resolvedTaskNo, student.getStudentNo());
        int maxAttempts = task.getMaxAttempts() != null && task.getMaxAttempts() > 0 ? task.getMaxAttempts() : 3;
        if (existingCount >= maxAttempts) {
            return Result.fail("已达最大提交次数（" + maxAttempts + "次），如需修改请联系教师");
        }

        // 覆盖旧提交：将学生之前对该任务的所有提交标记为 superseded
        submissionService.supersedePrevious(resolvedTaskNo, student.getStudentNo());

        // 校验附件格式
        if (file != null && !file.isEmpty() && task.getAttachmentFormats() != null
                && !task.getAttachmentFormats().isEmpty()) {
            String filename = file.getOriginalFilename();
            if (filename != null && filename.contains(".")) {
                String ext = "." + filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
                List<String> allowed = Arrays.asList(task.getAttachmentFormats().toLowerCase().split(","));
                if (!allowed.contains(ext)) {
                    return Result.fail("不允许的文件格式，仅支持：" + task.getAttachmentFormats());
                }
            }
        }

        // 检查内容：视频/阅读类不需要手动提交
        if ("video".equals(task.getTaskType()) || "reading".equals(task.getTaskType())) {
            return Result.fail("该类型任务由系统自动记录完成状态，无需手动提交");
        }

        if (content == null && file == null) return Result.fail("提交内容不能为空");

        TaskSubmission sub = new TaskSubmission();
        sub.setTaskNo(resolvedTaskNo);
        sub.setStudentNo(student.getStudentNo());
        sub.setAttemptNumber(existingCount + 1);
        sub.setContent(content);
        sub.setSubmitTime(LocalDateTime.now());
        sub.setIsOverdue(isOverdue ? 1 : 0);

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

    /** 查看某任务的所有提交（含学生名、任务类型）| admin/授课教师
     *  新路由: GET /api/tasks/{taskNo}/submissions  旧路由: GET /submission/task/{taskNo} */
    @GetMapping({"/api/tasks/{taskNo}/submissions", "/submission/task/{taskNo}"})
    public Result<List<TaskSubmissionDTO>> listByTask(@PathVariable String taskNo, HttpSession session) {
        LearningTask task = taskService.getById(taskNo);
        if (task == null) return Result.fail("任务不存在");
        if (!auth.canModifyCourse(session, task.getCourseCode())) return Result.fail("无权限");
        return Result.ok(submissionService.listDtoByTaskNo(taskNo));
    }

    /** 查看某学生某课程的提交 | admin/授课教师/学生本人
     *  GET /api/students/{studentNo}/submissions?courseCode= */
    @GetMapping("/api/students/{studentNo}/submissions")
    public Result<List<TaskSubmission>> listByStudent(@PathVariable String studentNo,
                                                       @RequestParam(required = false) String courseCode,
                                                       HttpSession session) {
        Student loginStudent = (Student) session.getAttribute("student");
        if (!auth.isAdmin(session) && !auth.isTeacher(session)
                && (loginStudent == null || !loginStudent.getStudentNo().equals(studentNo)))
            return Result.fail("无权限");
        List<TaskSubmission> subs = submissionService.listByStudentNo(studentNo);
        if (courseCode != null && !courseCode.isEmpty()) {
            subs = subs.stream()
                    .filter(s -> courseCode.equals(submissionService.getTaskCourseCode(s.getTaskNo())))
                    .collect(java.util.stream.Collectors.toList());
        }
        return Result.ok(subs);
    }

    /** 查看我的提交 | student
     *  GET /api/submissions/my  /submission/my */
    @GetMapping({"/api/submissions/my", "/submission/my"})
    public Result<List<TaskSubmission>> listMy(HttpSession session) {
        Student student = (Student) session.getAttribute("student");
        if (student == null) return Result.fail("请先登录学生账号");
        return Result.ok(submissionService.listByStudentNo(student.getStudentNo()));
    }

    /** 批改详情——含题目、学生答案、正确答案 | admin/授课教师
     *  GET /api/submissions/{submissionId}/grade */
    @GetMapping({"/api/submissions/{submissionId}/grade", "/submission/grade/{submissionId}"})
    public Result<Map<String, Object>> gradeDetail(@PathVariable String submissionId, HttpSession session) {
        TaskSubmission sub = submissionService.getById(submissionId);
        if (sub == null) return Result.fail("提交记录不存在");
        if (!auth.canModifyCourse(session, taskService.getById(sub.getTaskNo()).getCourseCode()))
            return Result.fail("无权限");
        return Result.ok(submissionService.buildGradeDetail(submissionId));
    }

    /** 批改打分 | admin/授课教师
     *  PUT /api/submissions/{submissionId} */
    @PutMapping({"/api/submissions/{submissionId}", "/submission/{submissionId}"})
    public Result<Void> grade(@PathVariable String submissionId,
                              @RequestBody TaskSubmission body, HttpSession session) {
        TaskSubmission sub = submissionService.getById(submissionId);
        if (sub == null) return Result.fail("提交记录不存在");
        String code = taskService.getById(sub.getTaskNo()).getCourseCode();
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

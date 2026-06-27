package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.TaskUpdateRequest;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.service.FileStorageService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.StudentService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import com.neu.CoursePlatform.entity.TaskSubmission;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final LearningTaskService taskService;
    private final FileStorageService fileStorageService;
    private final TaskSubmissionService submissionService;
    private final StudentService studentService;
    private final Auth auth;

    public TaskController(LearningTaskService taskService, FileStorageService fileStorageService,
                          TaskSubmissionService submissionService, StudentService studentService, Auth auth) {
        this.taskService = taskService;
        this.fileStorageService = fileStorageService;
        this.submissionService = submissionService;
        this.studentService = studentService;
        this.auth = auth;
    }

    /** 查看某课程的任务列表（支持筛选）| 登录用户 */
    @GetMapping
    public Result<List<LearningTask>> listByContract(@RequestParam("course_id") String courseId,
                                                     @RequestParam(required = false) String student_id,
                                                     @RequestParam(required = false) String taskType,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String lessonNo,
                                                     HttpSession session) {
        return list(courseId, taskType, status, lessonNo, session);
    }

    /** 查看某课程的任务列表（兼容旧前端路径）| 登录用户 */
    @GetMapping("/course/{courseCode}")
    public Result<List<LearningTask>> list(@PathVariable String courseCode,
                                           @RequestParam(required = false) String taskType,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String lessonNo,
                                           HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Map<String, String> filters = new HashMap<>();
        filters.put("courseCode", courseCode);
        if (taskType != null) filters.put("taskType", taskType);
        if (status != null) filters.put("status", status);
        if (lessonNo != null) filters.put("lessonNo", lessonNo);
        return Result.ok(taskService.listFiltered(filters));
    }

    /** 查看任务详情 | 登录用户 */
    @GetMapping({"/{taskNo}", "/detail/{taskNo}"})
    public Result<LearningTask> detail(@PathVariable String taskNo, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        LearningTask t = taskService.getById(taskNo);
        return t != null ? Result.ok(t) : Result.fail("任务不存在");
    }

    /** 模糊搜索任务 | 登录用户 */
    @GetMapping("/search")
    public Result<List<LearningTask>> search(@RequestParam String keyword, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(taskService.searchByKeyword(keyword));
    }

    /** 发布任务（支持全字段+附件上传）| admin/授课教师 */
    @PostMapping
    public Result<String> add(@RequestParam String courseCode,
                              @RequestParam(required = false) String taskName,
                              @RequestParam(required = false) String lessonNo,
                              @RequestParam(required = false) String knowledgePoints,
                              @RequestParam String taskType,
                              @RequestParam String description,
                              @RequestParam(required = false) String deadline,
                              @RequestParam(required = false, defaultValue = "") String submitMethod,
                              @RequestParam Integer score,
                              @RequestParam(required = false) String gradingRule,
                              @RequestParam(required = false, defaultValue = "published") String status,
                              @RequestParam(required = false, defaultValue = "0") Integer allowLate,
                              @RequestParam(required = false, defaultValue = "3") Integer maxAttempts,
                              @RequestParam(required = false) String attachmentFormats,
                              @RequestParam(required = false) MultipartFile file,
                              HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        LearningTask task = new LearningTask();
        task.setCourseCode(courseCode);
        task.setTaskName(taskName != null && !taskName.isBlank() ? taskName : description);
        task.setLessonNo(lessonNo);
        task.setKnowledgePoints(knowledgePoints);
        task.setTaskType(taskType);
        task.setDescription(description);
        task.setSubmitMethod(submitMethod);
        task.setScore(score);
        task.setGradingRule(gradingRule);
        task.setStatus(status);
        task.setAllowLate(allowLate);
        task.setMaxAttempts(maxAttempts);
        task.setAttachmentFormats(attachmentFormats);
        try {
            taskService.applyDeadline(task, deadline);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
        if (file != null && !file.isEmpty()) {
            try {
                task.setResourceUrl(fileStorageService.store(file, "../resource/TaskResource/"));
            } catch (IOException e) {
                return Result.fail("附件上传失败");
            }
        }
        taskService.save(task);
        return Result.ok(task.getTaskNo());
    }

    /** 修改任务（JSON body，支持附件）| admin/授课教师 */
    @PutMapping("/{courseCode}/{taskNo}")
    public Result<String> update(@PathVariable String courseCode, @PathVariable String taskNo,
                                 @RequestBody TaskUpdateRequest req,
                                 HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        LearningTask task = taskService.getById(taskNo);
        if (task == null) return Result.fail("任务不存在");
        if (req.getTaskName() != null) task.setTaskName(req.getTaskName());
        if (req.getLessonNo() != null) task.setLessonNo(req.getLessonNo());
        if (req.getKnowledgePoints() != null) task.setKnowledgePoints(req.getKnowledgePoints());
        if (req.getTaskType() != null) task.setTaskType(req.getTaskType());
        if (req.getDescription() != null) task.setDescription(req.getDescription());
        if (req.getSubmitMethod() != null) task.setSubmitMethod(req.getSubmitMethod());
        if (req.getScore() != null) task.setScore(req.getScore());
        if (req.getGradingRule() != null) task.setGradingRule(req.getGradingRule());
        if (req.getStatus() != null) task.setStatus(req.getStatus());
        if (req.getAllowLate() != null) task.setAllowLate(req.getAllowLate());
        if (req.getMaxAttempts() != null) task.setMaxAttempts(req.getMaxAttempts());
        if (req.getAttachmentFormats() != null) task.setAttachmentFormats(req.getAttachmentFormats());
        try {
            taskService.applyDeadline(task, req.getDeadline());
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
        taskService.updateById(task);
        return Result.ok("任务更新成功");
    }

    /** 修改任务（契约路由）| admin/授课教师
     *  PUT /api/tasks/{taskNo} */
    @PutMapping("/{taskNo}")
    public Result<String> updateByContract(@PathVariable String taskNo,
                                           @RequestBody TaskUpdateRequest req,
                                           HttpSession session) {
        LearningTask task = taskService.getById(taskNo);
        if (task == null) return Result.fail("任务不存在");
        return update(task.getCourseCode(), taskNo, req, session);
    }

    /** 切换任务状态 | admin/授课教师 */
    @PutMapping("/{courseCode}/{taskNo}/status")
    public Result<String> toggleStatus(@PathVariable String courseCode, @PathVariable String taskNo,
                                        @RequestBody Map<String, String> body, HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        LearningTask task = taskService.getById(taskNo);
        if (task == null) return Result.fail("任务不存在");
        String newStatus = body.get("status");
        if (newStatus == null || !newStatus.matches("draft|published|closed")) {
            return Result.fail("状态值无效，支持：draft / published / closed");
        }
        task.setStatus(newStatus);
        taskService.updateById(task);
        return Result.ok("任务状态已更新为 " + newStatus);
    }

    /** 删除任务 | admin/授课教师 */
    @DeleteMapping("/{courseCode}/{taskNo}")
    public Result<Map<String, Object>> delete(@PathVariable String courseCode, @PathVariable String taskNo,
                                               @RequestParam(required = false, defaultValue = "false") boolean confirm,
                                               HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        boolean hasSubmissions = taskService.hasSubmissions(taskNo);
        Map<String, Object> result = new HashMap<>();
        if (hasSubmissions && !confirm) {
            result.put("hasSubmissions", true);
            result.put("message", "该任务下有学生提交记录，确认删除吗？提交记录不会被物理删除。");
            return Result.ok(result);
        }
        taskService.removeById(taskNo);
        result.put("hasSubmissions", hasSubmissions);
        result.put("message", "任务已删除");
        return Result.ok(result);
    }

    /** 删除任务（契约路由）| admin/授课教师
     *  DELETE /api/tasks/{taskNo} */
    @DeleteMapping("/{taskNo}")
    public Result<Map<String, Object>> deleteByContract(@PathVariable String taskNo,
                                                        @RequestParam(required = false, defaultValue = "false") boolean confirm,
                                                        HttpSession session) {
        LearningTask task = taskService.getById(taskNo);
        if (task == null) return Result.fail("任务不存在");
        return delete(task.getCourseCode(), taskNo, confirm, session);
    }

    /** 任务完成统计 | admin/授课教师
     *  GET /api/tasks/{taskNo}/stats */
    @GetMapping("/{courseCode}/{taskNo}/stats")
    public Result<Map<String, Object>> taskStats(@PathVariable String courseCode, @PathVariable String taskNo,
                                                  HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        List<TaskSubmission> subs = submissionService.listByTaskNo(taskNo);
        List<TaskSubmission> activeSubs = subs.stream()
                .filter(s -> !"superseded".equals(s.getStatus()))
                .toList();
        int totalSubmissions = activeSubs.size();
        long totalStudents = studentService.count();
        long submittedStudents = activeSubs.stream()
                .map(TaskSubmission::getStudentNo)
                .filter(studentNo -> studentNo != null && !studentNo.isBlank())
                .distinct()
                .count();
        int graded = (int) activeSubs.stream().filter(s -> "graded".equals(s.getStatus())).count();
        int overdue = (int) activeSubs.stream().filter(s -> s.getIsOverdue() != null && s.getIsOverdue() == 1).count();
        double avgScore = activeSubs.stream()
                .filter(s -> "graded".equals(s.getStatus()) && s.getScore() != null)
                .mapToInt(TaskSubmission::getScore).average().orElse(0);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("taskNo", taskNo);
        stats.put("totalStudents", totalStudents);
        stats.put("submittedStudents", submittedStudents);
        stats.put("totalSubmissions", totalSubmissions);
        stats.put("gradedCount", graded);
        stats.put("overdueCount", overdue);
        stats.put("averageScore", Math.round(avgScore * 10) / 10.0);
        stats.put("completionRate", totalStudents > 0
                ? Math.round(submittedStudents * 1000.0 / totalStudents) / 10.0
                : 0);
        return Result.ok(stats);
    }

    /** 任务完成统计（契约路由）| admin/授课教师
     *  GET /api/tasks/{taskNo}/stats */
    @GetMapping("/{taskNo}/stats")
    public Result<Map<String, Object>> taskStatsByContract(@PathVariable String taskNo,
                                                           HttpSession session) {
        LearningTask task = taskService.getById(taskNo);
        if (task == null) return Result.fail("任务不存在");
        return taskStats(task.getCourseCode(), taskNo, session);
    }
}

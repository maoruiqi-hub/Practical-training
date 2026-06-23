package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.service.FileStorageService;
import com.neu.CoursePlatform.service.LearningTaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/task")
public class TaskController {

    private final LearningTaskService taskService;
    private final FileStorageService fileStorageService;
    private final Auth auth;

    public TaskController(LearningTaskService taskService, FileStorageService fileStorageService, Auth auth) {
        this.taskService = taskService;
        this.fileStorageService = fileStorageService;
        this.auth = auth;
    }

    /** 查看某课程的任务列表 | 登录用户 */
    @GetMapping("/{courseCode}")
    public Result<List<LearningTask>> list(@PathVariable String courseCode, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(taskService.listByCourseCode(courseCode));
    }

    /** 查看任务详情 | 登录用户 */
    @GetMapping("/detail/{taskNo}")
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

    /** 发布任务（支持上传附件） | admin/授课教师 */
    @PostMapping
    public Result<String> add(@RequestParam String courseCode,
                              @RequestParam String taskType,
                              @RequestParam String description,
                              @RequestParam(required = false) String deadline,
                              @RequestParam String submitMethod,
                              @RequestParam Integer score,
                              @RequestParam(required = false) MultipartFile file,
                              HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        LearningTask task = new LearningTask();
        task.setCourseCode(courseCode);
        task.setTaskType(taskType);
        task.setDescription(description);
        task.setSubmitMethod(submitMethod);
        task.setScore(score);
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

    /** 修改任务（支持上传附件） | admin/授课教师 */
    @PutMapping("/{courseCode}/{taskNo}")
    public Result<String> update(@PathVariable String courseCode, @PathVariable String taskNo,
                                 @RequestParam String taskType,
                                 @RequestParam String description,
                                 @RequestParam(required = false) String deadline,
                                 @RequestParam String submitMethod,
                                 @RequestParam Integer score,
                                 @RequestParam(required = false) MultipartFile file,
                                 HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        LearningTask task = taskService.getById(taskNo);
        if (task == null) return Result.fail("任务不存在");
        task.setTaskType(taskType);
        task.setDescription(description);
        task.setSubmitMethod(submitMethod);
        task.setScore(score);
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
        taskService.updateById(task);
        return Result.ok("任务更新成功");
    }

    /** 删除任务 | admin/授课教师 */
    @DeleteMapping("/{courseCode}/{taskNo}")
    public Result<Void> delete(@PathVariable String courseCode, @PathVariable String taskNo,
                               HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        taskService.removeById(taskNo);
        return Result.ok();
    }
}

package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.service.LearningTaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/task")
public class TaskController {

    private final LearningTaskService taskService;
    private final Auth auth;

    public TaskController(LearningTaskService taskService, Auth auth) {
        this.taskService = taskService;
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
        if (deadline != null && !deadline.isEmpty()) {
            task.setDeadline(LocalDateTime.parse(deadline.replace(" ", "T")));
        }
        if (file != null && !file.isEmpty()) {
            String dir = "../resource/TaskResource/";
            File folder = new File(dir);
            if (!folder.exists()) folder.mkdirs();
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            try {
                java.nio.file.Files.write(new File(dir + filename).toPath(), file.getBytes());
            } catch (IOException e) {
                return Result.fail("附件上传失败");
            }
            task.setResourceUrl((dir + filename).replace("../", ""));
        }
        taskService.save(task);
        return Result.ok("任务发布成功");
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
        if (deadline != null && !deadline.isEmpty()) {
            task.setDeadline(LocalDateTime.parse(deadline.replace(" ", "T")));
        }
        if (file != null && !file.isEmpty()) {
            try {
                String dir = "../resource/TaskResource/";
                File folder = new File(dir);
                if (!folder.exists()) folder.mkdirs();
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                java.nio.file.Files.write(new File(dir + filename).toPath(), file.getBytes());
                task.setResourceUrl((dir + filename).replace("../", ""));
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

package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.service.LearningTaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /** 模糊搜索任务 | 登录用户 */
    @GetMapping("/search")
    public Result<List<LearningTask>> search(@RequestParam String keyword, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(taskService.searchByKeyword(keyword));
    }

    /** 发布任务 | admin/授课教师 */
    @PostMapping
    public Result<Void> add(@RequestBody LearningTask task, HttpSession session) {
        if (!auth.canModifyCourse(session, task.getCourseCode())) return Result.fail("无权限");
        taskService.save(task);
        return Result.ok();
    }

    /** 修改任务 | admin/授课教师 */
    @PutMapping("/{courseCode}/{taskNo}")
    public Result<Void> update(@PathVariable String courseCode, @PathVariable String taskNo,
                               @RequestBody LearningTask task, HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        task.setTaskNo(taskNo);
        taskService.updateById(task);
        return Result.ok();
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

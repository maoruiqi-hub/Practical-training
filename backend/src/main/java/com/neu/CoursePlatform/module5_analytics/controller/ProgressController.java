package com.neu.CoursePlatform.module5_analytics.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.module5_analytics.dto.ClassProgressDTO;
import com.neu.CoursePlatform.module5_analytics.dto.TaskCompletionDTO;
import com.neu.CoursePlatform.module5_analytics.service.ProgressService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

/**
 * 学习进度监控 Controller（R3 组需求, T5）
 */
@RestController
@RequestMapping("/api/classes/{classId}")
public class ProgressController {

    private final ProgressService progressService;
    private final Auth auth;

    public ProgressController(ProgressService progressService, Auth auth) {
        this.progressService = progressService;
        this.auth = auth;
    }

    /** R3.1 班级整体学习进度 */
    @GetMapping("/progress")
    public Result<ClassProgressDTO> getProgress(@PathVariable String classId,
                                                 @RequestParam String courseId,
                                                 HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        return Result.ok(progressService.getClassProgress(classId, courseId));
    }

    /** R3.2 任务完成率详情 */
    @GetMapping("/task-completion")
    public Result<TaskCompletionDTO> getTaskCompletion(@PathVariable String classId,
                                                        @RequestParam String taskId,
                                                        HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        return Result.ok(progressService.getTaskCompletion(classId, taskId));
    }
}

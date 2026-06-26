package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.LearningBehaviorLog;
import com.neu.CoursePlatform.service.BehaviorLogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class BehaviorLogController {

    private final BehaviorLogService behaviorLogService;
    private final Auth auth;

    public BehaviorLogController(BehaviorLogService behaviorLogService, Auth auth) {
        this.behaviorLogService = behaviorLogService;
        this.auth = auth;
    }

    /** 记录一条学习行为日志 | 登录用户
     *  POST /api/learning-logs */
    @PostMapping({"/api/learning-logs", "/learning-logs"})
    public Result<Void> record(@RequestBody LearningBehaviorLog log, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        behaviorLogService.record(log);
        return Result.ok();
    }

    /** 查询行为日志（多条件筛选）| 登录用户
     *  GET /api/learning-logs?student_id=&course_id=&action_type=&start_time=&end_time= */
    @GetMapping({"/api/learning-logs", "/learning-logs"})
    public Result<List<LearningBehaviorLog>> query(@RequestParam(required = false) String student_id,
                                                    @RequestParam(required = false) String course_id,
                                                    @RequestParam(required = false) String action_type,
                                                    @RequestParam(required = false) String actionType,
                                                    @RequestParam(required = false) String resourceType,
                                                    @RequestParam(required = false) String start_time,
                                                    @RequestParam(required = false) String startTime,
                                                    @RequestParam(required = false) String end_time,
                                                    @RequestParam(required = false) String endTime,
                                                    @RequestParam(required = false) String userId,
                                                    @RequestParam(required = false) String userType,
                                                    HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Map<String, String> filters = new HashMap<>();
        if (student_id != null) filters.put("userId", student_id);
        if (course_id != null) filters.put("course_id", course_id);
        if (userId != null) filters.put("userId", userId);
        if (userType != null) filters.put("userType", userType);
        String resolvedActionType = actionType != null ? actionType : action_type;
        if (resolvedActionType != null) filters.put("actionType", resolvedActionType);
        if (resourceType != null) filters.put("resourceType", resourceType);
        String resolvedStartTime = startTime != null ? startTime : start_time;
        String resolvedEndTime = endTime != null ? endTime : end_time;
        if (resolvedStartTime != null) filters.put("startTime", resolvedStartTime);
        if (resolvedEndTime != null) filters.put("endTime", resolvedEndTime);
        return Result.ok(behaviorLogService.query(filters));
    }

    /** 按用户查询行为日志 | 登录用户 */
    @GetMapping({"/api/learning-logs/user/{userId}", "/learning-logs/user/{userId}"})
    public Result<List<LearningBehaviorLog>> byUser(@PathVariable String userId, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(behaviorLogService.listByUserId(userId));
    }

    /** 按任务查询行为日志 | 登录用户 */
    @GetMapping({"/api/learning-logs/task/{taskNo}", "/learning-logs/task/{taskNo}"})
    public Result<List<LearningBehaviorLog>> byTask(@PathVariable String taskNo, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(behaviorLogService.listByTaskNo(taskNo));
    }
}

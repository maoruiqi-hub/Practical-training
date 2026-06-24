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
@RequestMapping("/learning-logs")
public class BehaviorLogController {

    private final BehaviorLogService behaviorLogService;
    private final Auth auth;

    public BehaviorLogController(BehaviorLogService behaviorLogService, Auth auth) {
        this.behaviorLogService = behaviorLogService;
        this.auth = auth;
    }

    /** 记录一条学习行为日志 | 登录用户 */
    @PostMapping
    public Result<Void> record(@RequestBody LearningBehaviorLog log, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        behaviorLogService.record(log);
        return Result.ok();
    }

    /** 查询行为日志（多条件筛选）| 登录用户 */
    @GetMapping
    public Result<List<LearningBehaviorLog>> query(@RequestParam(required = false) String userId,
                                                    @RequestParam(required = false) String userType,
                                                    @RequestParam(required = false) String actionType,
                                                    @RequestParam(required = false) String resourceType,
                                                    @RequestParam(required = false) String startTime,
                                                    @RequestParam(required = false) String endTime,
                                                    HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Map<String, String> filters = new HashMap<>();
        if (userId != null) filters.put("userId", userId);
        if (userType != null) filters.put("userType", userType);
        if (actionType != null) filters.put("actionType", actionType);
        if (resourceType != null) filters.put("resourceType", resourceType);
        if (startTime != null) filters.put("startTime", startTime);
        if (endTime != null) filters.put("endTime", endTime);
        return Result.ok(behaviorLogService.query(filters));
    }

    /** 按用户查询行为日志 | 登录用户 */
    @GetMapping("/user/{userId}")
    public Result<List<LearningBehaviorLog>> byUser(@PathVariable String userId, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(behaviorLogService.listByUserId(userId));
    }

    /** 按任务查询行为日志 | 登录用户 */
    @GetMapping("/task/{taskNo}")
    public Result<List<LearningBehaviorLog>> byTask(@PathVariable String taskNo, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(behaviorLogService.listByTaskNo(taskNo));
    }
}

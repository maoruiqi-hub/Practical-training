package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.common.SharedIds;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.common.event.GameEventPublisher;
import com.neu.CoursePlatform.entity.LearningBehaviorLog;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.service.BehaviorLogService;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class BehaviorLogController {

    private final BehaviorLogService behaviorLogService;
    private final GameEventPublisher gameEventPublisher;
    private final CourseGameConfigService gameConfigService;
    private final Auth auth;

    @Autowired
    public BehaviorLogController(BehaviorLogService behaviorLogService,
                                 GameEventPublisher gameEventPublisher,
                                 CourseGameConfigService gameConfigService,
                                 Auth auth) {
        this.behaviorLogService = behaviorLogService;
        this.gameEventPublisher = gameEventPublisher;
        this.gameConfigService = gameConfigService;
        this.auth = auth;
    }

    public BehaviorLogController(BehaviorLogService behaviorLogService, Auth auth) {
        this(behaviorLogService, null, null, auth);
    }

    /** 记录一条学习行为日志 | 登录用户
     *  POST /api/learning-logs */
    @PostMapping({"/api/learning-logs", "/learning-logs"})
    public Result<Void> record(@RequestBody LearningBehaviorLog log,
                               @RequestParam(name = "course_id", required = false) String courseId,
                               @RequestParam(required = false) String courseCode,
                               HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        behaviorLogService.record(log);
        publishSupplyOrHintEvent(log, firstNonBlank(courseId, courseCode), session);
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

    private void publishSupplyOrHintEvent(LearningBehaviorLog log, String courseId, HttpSession session) {
        if (log == null || courseId == null || gameEventPublisher == null || gameConfigService == null
                || !gameConfigService.isEnabled(courseId)) return;

        String eventType = resolveGameEventType(log);
        if (eventType == null) return;

        Student student = auth.getStudent(session);
        String studentId = firstNonBlank(log.getUserId(), student == null ? null : student.getStudentNo());
        if (studentId == null) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("action_type", log.getActionType());
        payload.put("resource_type", log.getResourceType());
        payload.put("resource_id", log.getResourceId());
        payload.put("task_no", log.getTaskNo());
        payload.put("supply_type", firstNonBlank(log.getResourceType(), log.getResult(), "supply"));
        payload.put("question_id", firstNonBlank(log.getResourceId(), log.getTaskNo(), ""));

        gameEventPublisher.publish(GameEvent.builder()
                .eventId(SharedIds.newId())
                .eventType(eventType)
                .studentId(studentId)
                .courseId(courseId)
                .sourceId(firstNonBlank(log.getResourceId(), log.getTaskNo(), log.getLogId()))
                .occurredAt(LocalDateTime.now())
                .payload(payload)
                .build());
    }

    private String resolveGameEventType(LearningBehaviorLog log) {
        String action = normalize(log.getActionType());
        String resourceType = normalize(log.getResourceType());
        if (matchesAny(action, GameEventTypes.SUPPLY_USED, "supply", "use_supply", "supply_used")
                || matchesAny(resourceType, "supply", "potion", "energy")) {
            return GameEventTypes.SUPPLY_USED;
        }
        if (matchesAny(action, GameEventTypes.HINT_USED, "hint", "use_hint", "hint_used")
                || matchesAny(resourceType, "hint", "tip")) {
            return GameEventTypes.HINT_USED;
        }
        return null;
    }

    private boolean matchesAny(String value, String... candidates) {
        if (value == null) return false;
        for (String candidate : candidates) {
            if (value.equals(normalize(candidate))) return true;
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}

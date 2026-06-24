package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.CourseGameConfigRequest;
import com.neu.CoursePlatform.dto.FloorStatusUpdateRequest;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgePointFloorStatus;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import com.neu.CoursePlatform.service.CourseService;
import com.neu.CoursePlatform.service.KnowledgePointFloorStatusService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/** Module-1 state owned by the tower contract; game rules remain in Module 4. */
@RestController
@RequestMapping("/api")
public class CourseGameController {

    private static final Set<String> FLOOR_STATUSES = Set.of("locked", "available", "cleared", "weak");
    private final CourseService courseService;
    private final CourseGameConfigService gameConfigService;
    private final KnowledgePointService knowledgePointService;
    private final KnowledgePointFloorStatusService floorStatusService;
    private final Auth auth;

    public CourseGameController(CourseService courseService, CourseGameConfigService gameConfigService,
                                KnowledgePointService knowledgePointService,
                                KnowledgePointFloorStatusService floorStatusService, Auth auth) {
        this.courseService = courseService;
        this.gameConfigService = gameConfigService;
        this.knowledgePointService = knowledgePointService;
        this.floorStatusService = floorStatusService;
        this.auth = auth;
    }

    @GetMapping("/courses/{courseCode}/config")
    public Result<Map<String, Boolean>> config(@PathVariable String courseCode, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        if (courseService.getById(courseCode) == null) return Result.fail("课程不存在");
        return Result.ok(Map.of("game_mode_enabled", gameConfigService.isGameModeEnabled(courseCode)));
    }

    @PutMapping("/courses/{courseCode}/config")
    public Result<Void> updateConfig(@PathVariable String courseCode, @RequestBody CourseGameConfigRequest request,
                                     HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权修改课程游戏配置");
        if (request == null || request.getGameModeEnabled() == null) return Result.fail("gameModeEnabled 不能为空");
        gameConfigService.setGameModeEnabled(courseCode, request.getGameModeEnabled());
        return Result.ok();
    }

    @PutMapping("/knowledge-points/{knowledgePointId}/floor-status")
    public Result<KnowledgePointFloorStatus> updateFloorStatus(@PathVariable String knowledgePointId,
                                                                @RequestBody FloorStatusUpdateRequest request,
                                                                HttpSession session) {
        KnowledgePoint point = knowledgePointService.getById(knowledgePointId);
        if (point == null) return Result.fail("知识点不存在");
        if (!auth.canModifyCourse(session, point.getCourseCode())) return Result.fail("无权维护楼层状态");
        if (request == null || request.getStudentNo() == null || request.getStudentNo().isBlank()
                || !FLOOR_STATUSES.contains(request.getStatus())) return Result.fail("studentNo 或楼层状态不合法");
        return Result.ok(floorStatusService.updateStatus(request.getStudentNo(), point.getCourseCode(),
                knowledgePointId, request.getStatus()));
    }
}

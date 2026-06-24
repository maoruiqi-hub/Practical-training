package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.KnowledgeEdge;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.service.KnowledgeEdgeService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    private final KnowledgePointService pointService;
    private final KnowledgeEdgeService edgeService;
    private final Auth auth;

    public KnowledgeController(KnowledgePointService pointService, KnowledgeEdgeService edgeService, Auth auth) {
        this.pointService = pointService;
        this.edgeService = edgeService;
        this.auth = auth;
    }

    @GetMapping("/course/{courseCode}/points")
    public Result<List<KnowledgePoint>> listPoints(@PathVariable String courseCode,
                                                   @RequestParam(required = false) String lessonNo,
                                                   @RequestParam(required = false) String keyword,
                                                   HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(pointService.listByCourse(courseCode, lessonNo, keyword));
    }

    @PostMapping("/course/{courseCode}/points")
    public Result<Void> addPoint(@PathVariable String courseCode,
                                 @RequestBody KnowledgePoint point,
                                 HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        point.setCourseCode(courseCode);
        pointService.save(point);
        return Result.ok();
    }

    @PutMapping("/points/{pointId}")
    public Result<Void> updatePoint(@PathVariable String pointId,
                                    @RequestBody KnowledgePoint point,
                                    HttpSession session) {
        KnowledgePoint existing = pointService.getById(pointId);
        if (existing == null) return Result.fail("知识点不存在");
        if (!auth.canModifyCourse(session, existing.getCourseCode())) return Result.fail("无权限");
        point.setKnowledgePointId(pointId);
        point.setCourseCode(existing.getCourseCode());
        pointService.updateById(point);
        return Result.ok();
    }

    @DeleteMapping("/points/{pointId}")
    public Result<Void> deletePoint(@PathVariable String pointId, HttpSession session) {
        KnowledgePoint existing = pointService.getById(pointId);
        if (existing == null) return Result.fail("知识点不存在");
        if (!auth.canModifyCourse(session, existing.getCourseCode())) return Result.fail("无权限");
        try {
            pointService.removePoint(pointId);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
        return Result.ok();
    }

    @GetMapping("/course/{courseCode}/graph")
    public Result<Map<String, Object>> graph(@PathVariable String courseCode, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(Map.of(
                "points", pointService.listByCourse(courseCode, null, null),
                "edges", edgeService.listByCourse(courseCode)
        ));
    }

    @PostMapping("/course/{courseCode}/edges")
    public Result<Void> addEdge(@PathVariable String courseCode,
                                @RequestBody KnowledgeEdge edge,
                                HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        try {
            edgeService.createEdge(courseCode, edge);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
        return Result.ok();
    }

    @DeleteMapping("/edges/{edgeId}")
    public Result<Void> deleteEdge(@PathVariable String edgeId, HttpSession session) {
        KnowledgeEdge existing = edgeService.getById(edgeId);
        if (existing == null) return Result.fail("关系不存在");
        if (!auth.canModifyCourse(session, existing.getCourseCode())) return Result.fail("无权限");
        edgeService.removeById(edgeId);
        return Result.ok();
    }
}

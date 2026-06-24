package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgeRelation;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.KnowledgeRelationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class KnowledgeController {

    private final KnowledgePointService pointService;
    private final KnowledgeRelationService relationService;
    private final Auth auth;

    public KnowledgeController(KnowledgePointService pointService, KnowledgeRelationService relationService, Auth auth) {
        this.pointService = pointService;
        this.relationService = relationService;
        this.auth = auth;
    }

    @GetMapping("/knowledge-points")
    public Result<List<KnowledgePoint>> listPoints(@RequestParam String courseCode,
                                                   @RequestParam(required = false) String lessonNo,
                                                   @RequestParam(required = false) String keyword,
                                                   HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(pointService.listByCourse(courseCode, lessonNo, keyword));
    }

    @PostMapping("/knowledge-points")
    public Result<Void> addPoint(@RequestBody KnowledgePoint point,
                                 HttpSession session) {
        String courseCode = point.getCourseCode();
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        pointService.save(point);
        return Result.ok();
    }

    @PutMapping("/knowledge-points/{pointId}")
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

    @DeleteMapping("/knowledge-points/{pointId}")
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

    @GetMapping("/knowledge-graph")
    public Result<Map<String, Object>> graph(@RequestParam String courseCode, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(Map.of(
                "points", pointService.listByCourse(courseCode, null, null),
                "relations", relationService.listByCourse(courseCode)
        ));
    }

    @PostMapping("/knowledge-relations")
    public Result<Void> addRelation(@RequestBody KnowledgeRelation relation,
                                    HttpSession session) {
        String courseCode = relation.getCourseCode();
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        try {
            relationService.createRelation(courseCode, relation);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
        return Result.ok();
    }

    @DeleteMapping("/knowledge-relations/{relationId}")
    public Result<Void> deleteRelation(@PathVariable String relationId, HttpSession session) {
        KnowledgeRelation existing = relationService.getById(relationId);
        if (existing == null) return Result.fail("关系不存在");
        if (!auth.canModifyCourse(session, existing.getCourseCode())) return Result.fail("无权限");
        relationService.removeById(relationId);
        return Result.ok();
    }
}

package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.AbilityMapDTO;
import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.service.AbilityMapService;
import com.neu.CoursePlatform.service.AbilityPointService;
import com.neu.CoursePlatform.service.CourseService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/ability-map")
public class AbilityMapController {

    private static final int MAX_ABILITY_POINTS_PER_COURSE = 20;

    private final AbilityPointService abilityPointService;
    private final AbilityMapService abilityMapService;
    private final CourseService courseService;
    private final KnowledgePointService knowledgePointService;
    private final Auth auth;

    public AbilityMapController(AbilityPointService abilityPointService,
                                AbilityMapService abilityMapService,
                                CourseService courseService,
                                KnowledgePointService knowledgePointService,
                                Auth auth) {
        this.abilityPointService = abilityPointService;
        this.abilityMapService = abilityMapService;
        this.courseService = courseService;
        this.knowledgePointService = knowledgePointService;
        this.auth = auth;
    }

    @GetMapping
    public Result<AbilityMapDTO> get(@RequestParam String courseCode, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        if (courseService.getById(courseCode) == null) return Result.fail("课程不存在");
        return Result.ok(abilityMapService.getByCourseCode(courseCode));
    }

    @PostMapping
    public Result<String> create(@RequestBody AbilityPoint point, HttpSession session) {
        if (point == null || isBlank(point.getCourseCode()) || isBlank(point.getName())) {
            return Result.fail("课程编号和能力点名称不能为空");
        }
        if (courseService.getById(point.getCourseCode()) == null) return Result.fail("课程不存在");
        if (!auth.canModifyCourse(session, point.getCourseCode())) return Result.fail("无权维护该课程能力图谱");
        point.setAbilityPointId(null);
        point.setName(point.getName().trim());
        List<AbilityPoint> existingPoints = abilityPointService.listByCourseCode(point.getCourseCode());
        if (hasSameName(existingPoints, point.getName(), null)) {
            return Result.fail("该课程已存在同名能力点");
        }
        if (existingPoints.size() >= MAX_ABILITY_POINTS_PER_COURSE) {
            return Result.fail("每门课程最多只能创建20个能力点");
        }
        abilityPointService.save(point);
        return Result.ok(point.getAbilityPointId());
    }

    @PutMapping("/{abilityPointId}")
    public Result<Void> update(@PathVariable String abilityPointId,
                               @RequestBody AbilityPoint request,
                               HttpSession session) {
        AbilityPoint existing = abilityPointService.getById(abilityPointId);
        if (existing == null) return Result.fail("能力点不存在");
        if (!auth.canModifyCourse(session, existing.getCourseCode())) return Result.fail("无权维护该课程能力图谱");
        if (request == null || isBlank(request.getName())) return Result.fail("能力点名称不能为空");
        String nextName = request.getName().trim();
        if (hasSameName(abilityPointService.listByCourseCode(existing.getCourseCode()), nextName, abilityPointId)) {
            return Result.fail("该课程已存在同名能力点");
        }
        existing.setName(nextName);
        existing.setDescription(request.getDescription());
        abilityPointService.updateById(existing);
        return Result.ok();
    }

    @DeleteMapping("/{abilityPointId}")
    public Result<Void> delete(@PathVariable String abilityPointId, HttpSession session) {
        AbilityPoint abilityPoint = abilityPointService.getById(abilityPointId);
        if (abilityPoint == null) return Result.fail("能力点不存在");
        if (!auth.canModifyCourse(session, abilityPoint.getCourseCode())) return Result.fail("无权维护该课程能力图谱");
        abilityMapService.deleteAbilityPoint(abilityPointId);
        return Result.ok();
    }

    @PostMapping("/{abilityPointId}/knowledge-points/{knowledgePointId}")
    public Result<Void> bind(@PathVariable String abilityPointId,
                             @PathVariable String knowledgePointId,
                             HttpSession session) {
        AbilityPoint abilityPoint = abilityPointService.getById(abilityPointId);
        KnowledgePoint knowledgePoint = knowledgePointService.getById(knowledgePointId);
        if (abilityPoint == null || knowledgePoint == null) return Result.fail("能力点或知识点不存在");
        if (!abilityPoint.getCourseCode().equals(knowledgePoint.getCourseCode())) return Result.fail("不能跨课程映射");
        if (!auth.canModifyCourse(session, abilityPoint.getCourseCode())) return Result.fail("无权维护该课程能力图谱");
        abilityMapService.bindKnowledgePoint(abilityPointId, knowledgePointId);
        return Result.ok();
    }

    @DeleteMapping("/{abilityPointId}/knowledge-points/{knowledgePointId}")
    public Result<Void> unbind(@PathVariable String abilityPointId,
                               @PathVariable String knowledgePointId,
                               HttpSession session) {
        AbilityPoint abilityPoint = abilityPointService.getById(abilityPointId);
        if (abilityPoint == null) return Result.fail("能力点不存在");
        if (!auth.canModifyCourse(session, abilityPoint.getCourseCode())) return Result.fail("无权维护该课程能力图谱");
        abilityMapService.unbindKnowledgePoint(abilityPointId, knowledgePointId);
        return Result.ok();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean hasSameName(List<AbilityPoint> points, String name, String excludedId) {
        String normalizedName = normalizeName(name);
        return points.stream().anyMatch(point ->
                !String.valueOf(point.getAbilityPointId()).equals(String.valueOf(excludedId))
                        && normalizeName(point.getName()).equals(normalizedName));
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

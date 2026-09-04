package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.AbilityCompetencyMapDTO;
import com.neu.CoursePlatform.dto.AbilityCompetencyRelationRequest;
import com.neu.CoursePlatform.entity.CompetencyPoint;
import com.neu.CoursePlatform.entity.CompetencyTaskObservation;
import com.neu.CoursePlatform.service.AbilityCompetencyMappingService;
import com.neu.CoursePlatform.service.CourseService;
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

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/ability-competency-map")
public class AbilityCompetencyMappingController {
    private final AbilityCompetencyMappingService mappingService;
    private final CourseService courseService;
    private final Auth auth;

    public AbilityCompetencyMappingController(AbilityCompetencyMappingService mappingService,
                                               CourseService courseService,
                                               Auth auth) {
        this.mappingService = mappingService;
        this.courseService = courseService;
        this.auth = auth;
    }

    @GetMapping
    public Result<AbilityCompetencyMapDTO> get(@RequestParam String courseCode, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        if (courseService.getById(courseCode) == null) return Result.fail("课程不存在");
        return Result.ok(mappingService.getByCourseCode(courseCode));
    }

    @PostMapping("/competencies")
    public Result<CompetencyPoint> create(@RequestBody CompetencyPoint point, HttpSession session) {
        if (point == null || point.getCourseCode() == null || point.getName() == null || point.getName().isBlank()) {
            return Result.fail("课程编号和真能力名称不能为空");
        }
        if (courseService.getById(point.getCourseCode()) == null) return Result.fail("课程不存在");
        if (!auth.canModifyCourse(session, point.getCourseCode())) return Result.fail("无权维护该课程能力映射");
        try {
            return Result.ok(mappingService.createCompetency(point));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/competencies/{competencyId}")
    public Result<Void> update(@PathVariable String competencyId, @RequestBody CompetencyPoint point, HttpSession session) {
        if (point == null || point.getName() == null || point.getName().isBlank()) return Result.fail("真能力名称不能为空");
        CompetencyPoint existing = mappingService.getCompetencyById(competencyId);
        if (existing == null) return Result.fail("真能力不存在");
        if (point.getCourseCode() != null && !point.getCourseCode().isBlank()
                && !existing.getCourseCode().equals(point.getCourseCode())) {
            return Result.fail("不能跨课程修改真能力");
        }
        if (!auth.canModifyCourse(session, existing.getCourseCode())) return Result.fail("无权维护该课程能力映射");
        try {
            return mappingService.updateCompetency(competencyId, point) ? Result.ok() : Result.fail("保存失败");
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/competencies/{competencyId}")
    public Result<Void> delete(@PathVariable String competencyId, @RequestParam String courseCode, HttpSession session) {
        CompetencyPoint existing = mappingService.getCompetencyById(competencyId);
        if (existing == null || !courseCode.equals(existing.getCourseCode())) return Result.fail("真能力不属于当前课程");
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权维护该课程能力映射");
        return mappingService.deleteCompetency(competencyId) ? Result.ok() : Result.fail("真能力不存在");
    }

    @PutMapping("/relations")
    public Result<Void> relation(@RequestBody AbilityCompetencyRelationRequest request, HttpSession session) {
        if (request == null || request.getCourseCode() == null || request.getAbilityPointId() == null || request.getCompetencyId() == null) {
            return Result.fail("映射关系参数不完整");
        }
        if (!"related".equalsIgnoreCase(request.getRelationStatus())
                && !"unrelated".equalsIgnoreCase(request.getRelationStatus())
                && !"uncertain".equalsIgnoreCase(request.getRelationStatus())) {
            return Result.fail("关系状态只能是 related、unrelated 或 uncertain");
        }
        if (!auth.canModifyCourse(session, request.getCourseCode())) return Result.fail("无权维护该课程能力映射");
        try {
            mappingService.saveRelation(request);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
        return Result.ok();
    }

    @PutMapping("/observations")
    public Result<Void> observation(@RequestBody CompetencyTaskObservation observation, HttpSession session) {
        if (observation == null || observation.getCourseCode() == null || observation.getTaskNo() == null
                || observation.getCompetencyId() == null) return Result.fail("观测任务参数不完整");
        if (!auth.canModifyCourse(session, observation.getCourseCode())) return Result.fail("无权维护该课程能力映射");
        try {
            mappingService.saveObservation(observation);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
        return Result.ok();
    }

    @PutMapping("/observations/batch")
    public Result<Void> observationsBatch(@RequestBody List<CompetencyTaskObservation> observations, HttpSession session) {
        if (observations == null || observations.isEmpty()) return Result.fail("观测任务参数不能为空");
        String courseCode = observations.get(0) == null ? null : observations.get(0).getCourseCode();
        if (courseCode == null || courseCode.isBlank()) return Result.fail("课程编号不能为空");
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权维护该课程能力映射");
        try {
            mappingService.saveObservations(observations);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
        return Result.ok();
    }

    @PostMapping("/calibrate")
    public Result<Map<String, Object>> calibrate(@RequestParam String courseCode, HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权校准该课程能力映射");
        return Result.ok(mappingService.calibrateStrengths(courseCode));
    }

    @PostMapping("/publish")
    public Result<Void> publish(@RequestParam String courseCode, @RequestParam String version, HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权发布该课程能力映射");
        try {
            mappingService.publishVersion(courseCode, version, auth.getTeacherId(session));
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }
}

package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.KnowledgeGraphDTO;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgeRelation;
import com.neu.CoursePlatform.service.CourseService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.KnowledgeRelationService;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class KnowledgePointController {

    private final KnowledgePointService knowledgePointService;
    private final KnowledgeRelationService knowledgeRelationService;
    private final CourseService courseService;
    private final Auth auth;

    public KnowledgePointController(KnowledgePointService knowledgePointService,
                                    KnowledgeRelationService knowledgeRelationService,
                                    CourseService courseService,
                                    Auth auth) {
        this.knowledgePointService = knowledgePointService;
        this.knowledgeRelationService = knowledgeRelationService;
        this.courseService = courseService;
        this.auth = auth;
    }

    @GetMapping("/knowledge-points")
    public Result<List<KnowledgePoint>> list(@RequestParam(required = false) String courseCode,
                                             @RequestParam(name = "course_id", required = false) String courseId,
                                             @RequestParam(required = false) String chapter,
                                             HttpSession session) {
        courseCode = courseCode == null || courseCode.isBlank() ? courseId : courseCode;
        if (courseCode == null || courseCode.isBlank()) return Result.fail("course_id 不能为空");
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        if (courseService.getById(courseCode) == null) return Result.fail("课程不存在");
        return Result.ok(knowledgePointService.listByCourseCode(courseCode, chapter));
    }

    @GetMapping("/knowledge-points/{knowledgePointId}")
    public Result<KnowledgePoint> detail(@PathVariable String knowledgePointId, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        KnowledgePoint knowledgePoint = knowledgePointService.getById(knowledgePointId);
        return knowledgePoint == null ? Result.fail("知识点不存在") : Result.ok(knowledgePoint);
    }

    @GetMapping("/knowledge-graph")
    public Result<KnowledgeGraphDTO> graph(@RequestParam(required = false) String courseCode,
                                           @RequestParam(name = "course_id", required = false) String courseId,
                                           HttpSession session) {
        courseCode = courseCode == null || courseCode.isBlank() ? courseId : courseCode;
        if (courseCode == null || courseCode.isBlank()) return Result.fail("course_id 不能为空");
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        if (courseService.getById(courseCode) == null) return Result.fail("课程不存在");
        return Result.ok(new KnowledgeGraphDTO(
                knowledgePointService.listByCourseCode(courseCode, null),
                knowledgeRelationService.listByCourseCode(courseCode)));
    }

    @GetMapping("/courses/{courseCode}/structure")
    public Result<List<KnowledgePoint>> structure(@PathVariable String courseCode, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        if (courseService.getById(courseCode) == null) return Result.fail("课程不存在");

        List<KnowledgePoint> points = knowledgePointService.listByCourseCode(courseCode, null);
        Map<String, KnowledgePoint> pointsById = new HashMap<>();
        for (KnowledgePoint point : points) {
            point.setChildren(new ArrayList<>());
            pointsById.put(point.getKnowledgePointId(), point);
        }

        Set<String> childIds = new HashSet<>();
        for (KnowledgeRelation relation : knowledgeRelationService.listByCourseCode(courseCode)) {
            if (!"hierarchy".equals(relation.getRelationType())) continue;
            KnowledgePoint parent = pointsById.get(relation.getFromKnowledgePointId());
            KnowledgePoint child = pointsById.get(relation.getToKnowledgePointId());
            if (parent != null && child != null) {
                parent.getChildren().add(child);
                childIds.add(child.getKnowledgePointId());
            }
        }

        List<KnowledgePoint> roots = new ArrayList<>();
        for (KnowledgePoint point : points) {
            if (!childIds.contains(point.getKnowledgePointId())) roots.add(point);
        }
        return Result.ok(roots);
    }

    @GetMapping("/knowledge-points/{knowledgePointId}/prerequisites")
    public Result<List<KnowledgePoint>> prerequisites(@PathVariable String knowledgePointId, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        KnowledgePoint target = knowledgePointService.getById(knowledgePointId);
        if (target == null) return Result.fail("知识点不存在");

        Map<String, KnowledgePoint> pointsById = new HashMap<>();
        for (KnowledgePoint point : knowledgePointService.listByCourseCode(target.getCourseCode(), null)) {
            pointsById.put(point.getKnowledgePointId(), point);
        }
        Map<String, List<String>> directPrerequisites = new HashMap<>();
        for (KnowledgeRelation relation : knowledgeRelationService.listByCourseCode(target.getCourseCode())) {
            if ("prerequisite".equals(relation.getRelationType())) {
                directPrerequisites.computeIfAbsent(relation.getToKnowledgePointId(), ignored -> new ArrayList<>())
                        .add(relation.getFromKnowledgePointId());
            }
        }

        ArrayDeque<String> queue = new ArrayDeque<>();
        Set<String> prerequisiteIds = new LinkedHashSet<>();
        queue.add(knowledgePointId);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            for (String prerequisiteId : directPrerequisites.getOrDefault(current, List.of())) {
                if (prerequisiteIds.add(prerequisiteId)) queue.addLast(prerequisiteId);
            }
        }

        List<KnowledgePoint> result = new ArrayList<>();
        for (String prerequisiteId : prerequisiteIds) {
            KnowledgePoint prerequisite = pointsById.get(prerequisiteId);
            if (prerequisite != null) result.add(prerequisite);
        }
        return Result.ok(result);
    }

    @PostMapping("/knowledge-points")
    public Result<String> create(@RequestBody KnowledgePoint knowledgePoint, HttpSession session) {
        String validationMessage = validateForWrite(knowledgePoint);
        if (validationMessage != null) return Result.fail(validationMessage);
        if (courseService.getById(knowledgePoint.getCourseCode()) == null) return Result.fail("课程不存在");
        if (!auth.canModifyCourse(session, knowledgePoint.getCourseCode())) return Result.fail("无权限");

        knowledgePoint.setKnowledgePointId(null);
        knowledgePointService.save(knowledgePoint);
        return Result.ok("知识点创建成功");
    }

    @PutMapping("/knowledge-points/{knowledgePointId}")
    public Result<String> update(@PathVariable String knowledgePointId,
                                 @RequestBody KnowledgePoint knowledgePoint,
                                 HttpSession session) {
        KnowledgePoint existing = knowledgePointService.getById(knowledgePointId);
        if (existing == null) return Result.fail("知识点不存在");
        if (!auth.canModifyCourse(session, existing.getCourseCode())) return Result.fail("无权限");
        if (knowledgePoint.getCourseCode() != null && !existing.getCourseCode().equals(knowledgePoint.getCourseCode())) {
            return Result.fail("不支持跨课程移动知识点");
        }
        knowledgePoint.setCourseCode(existing.getCourseCode());
        knowledgePoint.setKnowledgePointId(knowledgePointId);
        String validationMessage = validateForWrite(knowledgePoint);
        if (validationMessage != null) return Result.fail(validationMessage);

        knowledgePointService.updateById(knowledgePoint);
        return Result.ok("知识点更新成功");
    }

    @DeleteMapping("/knowledge-points/{knowledgePointId}")
    public Result<Void> delete(@PathVariable String knowledgePointId, HttpSession session) {
        KnowledgePoint existing = knowledgePointService.getById(knowledgePointId);
        if (existing == null) return Result.fail("知识点不存在");
        if (!auth.canModifyCourse(session, existing.getCourseCode())) return Result.fail("无权限");

        knowledgePointService.deleteWithDependencies(knowledgePointId);
        return Result.ok();
    }

    private String validateForWrite(KnowledgePoint knowledgePoint) {
        if (knowledgePoint == null || knowledgePoint.getCourseCode() == null || knowledgePoint.getCourseCode().isBlank()) {
            return "课程编号不能为空";
        }
        if (knowledgePoint.getName() == null || knowledgePoint.getName().isBlank()) {
            return "知识点名称不能为空";
        }
        if (knowledgePoint.getImportance() != null && (knowledgePoint.getImportance() < 1 || knowledgePoint.getImportance() > 5)) {
            return "知识点重要程度必须在 1 到 5 之间";
        }
        return null;
    }
}

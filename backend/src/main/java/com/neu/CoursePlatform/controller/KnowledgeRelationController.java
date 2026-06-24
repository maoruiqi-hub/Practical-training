package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgeRelation;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.KnowledgeRelationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/knowledge-relation")
public class KnowledgeRelationController {

    private static final Set<String> RELATION_TYPES = Set.of("hierarchy", "prerequisite", "related");

    private final KnowledgeRelationService knowledgeRelationService;
    private final KnowledgePointService knowledgePointService;
    private final Auth auth;

    public KnowledgeRelationController(KnowledgeRelationService knowledgeRelationService,
                                       KnowledgePointService knowledgePointService,
                                       Auth auth) {
        this.knowledgeRelationService = knowledgeRelationService;
        this.knowledgePointService = knowledgePointService;
        this.auth = auth;
    }

    @GetMapping
    public Result<List<KnowledgeRelation>> list(@RequestParam String courseCode, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(knowledgeRelationService.listByCourseCode(courseCode));
    }

    @PostMapping
    public Result<String> create(@RequestBody KnowledgeRelation relation, HttpSession session) {
        String validationMessage = validate(relation);
        if (validationMessage != null) return Result.fail(validationMessage);

        KnowledgePoint from = knowledgePointService.getById(relation.getFromKnowledgePointId());
        KnowledgePoint to = knowledgePointService.getById(relation.getToKnowledgePointId());
        if (from == null || to == null) return Result.fail("关联的知识点不存在");
        if (!from.getCourseCode().equals(to.getCourseCode())) return Result.fail("不能关联不同课程的知识点");
        if (!auth.canModifyCourse(session, from.getCourseCode())) return Result.fail("无权限");

        relation.setCourseCode(from.getCourseCode());
        relation.setRelationType(relation.getRelationType().trim().toLowerCase());
        if (knowledgeRelationService.relationExists(relation.getCourseCode(), relation.getFromKnowledgePointId(),
                relation.getToKnowledgePointId(), relation.getRelationType())) {
            return Result.fail("知识点关系已存在");
        }
        if (knowledgeRelationService.wouldCreateCycle(relation.getCourseCode(), relation.getFromKnowledgePointId(),
                relation.getToKnowledgePointId(), relation.getRelationType())) {
            return Result.fail("该关系会形成知识点环");
        }

        relation.setRelationId(null);
        knowledgeRelationService.save(relation);
        return Result.ok("知识点关系创建成功");
    }

    @DeleteMapping("/{relationId}")
    public Result<Void> delete(@PathVariable String relationId, HttpSession session) {
        KnowledgeRelation relation = knowledgeRelationService.getById(relationId);
        if (relation == null) return Result.fail("知识点关系不存在");
        if (!auth.canModifyCourse(session, relation.getCourseCode())) return Result.fail("无权限");
        knowledgeRelationService.removeById(relationId);
        return Result.ok();
    }

    private String validate(KnowledgeRelation relation) {
        if (relation == null || relation.getFromKnowledgePointId() == null || relation.getFromKnowledgePointId().isBlank()
                || relation.getToKnowledgePointId() == null || relation.getToKnowledgePointId().isBlank()) {
            return "起始和目标知识点不能为空";
        }
        if (relation.getFromKnowledgePointId().equals(relation.getToKnowledgePointId())) {
            return "知识点不能关联自身";
        }
        if (relation.getRelationType() == null || !RELATION_TYPES.contains(relation.getRelationType().trim().toLowerCase())) {
            return "关系类型必须是 hierarchy、prerequisite 或 related";
        }
        return null;
    }
}

package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgeRelation;
import com.neu.CoursePlatform.mapper.KnowledgeRelationMapper;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.KnowledgeRelationService;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class KnowledgeRelationServiceImpl extends ServiceImpl<KnowledgeRelationMapper, KnowledgeRelation>
        implements KnowledgeRelationService {

    private final KnowledgePointService knowledgePointService;

    public KnowledgeRelationServiceImpl(KnowledgePointService knowledgePointService) {
        this.knowledgePointService = knowledgePointService;
    }

    @Override
    public List<KnowledgeRelation> listByCourse(String courseCode) {
        return listByCourseCode(courseCode);
    }

    @Override
    public void createRelation(String courseCode, KnowledgeRelation relation) {
        validateRelation(courseCode, relation);
        relation.setCourseCode(courseCode);
        if (relation.getRelationType() == null || relation.getRelationType().isBlank()) {
            relation.setRelationType("prerequisite");
        }
        relation.setRelationType(relation.getRelationType().trim().toLowerCase());
        if (relationExists(courseCode, relation.getFromKnowledgePointId(),
                relation.getToKnowledgePointId(), relation.getRelationType())) {
            throw new IllegalArgumentException("知识点关系已存在");
        }
        if (wouldCreateCycle(courseCode, relation.getFromKnowledgePointId(),
                relation.getToKnowledgePointId(), relation.getRelationType())) {
            throw new IllegalArgumentException("该关系会形成知识点环");
        }
        save(relation);
    }

    @Override
    public List<KnowledgeRelation> listByCourseCode(String courseCode) {
        return list(new LambdaQueryWrapper<KnowledgeRelation>()
                .eq(KnowledgeRelation::getCourseCode, courseCode)
                .orderByAsc(KnowledgeRelation::getRelationId));
    }

    @Override
    public boolean relationExists(String courseCode, String fromKnowledgePointId,
                                  String toKnowledgePointId, String relationType) {
        return count(new LambdaQueryWrapper<KnowledgeRelation>()
                .eq(KnowledgeRelation::getCourseCode, courseCode)
                .eq(KnowledgeRelation::getFromKnowledgePointId, fromKnowledgePointId)
                .eq(KnowledgeRelation::getToKnowledgePointId, toKnowledgePointId)
                .eq(KnowledgeRelation::getRelationType, relationType)) > 0;
    }

    @Override
    public boolean wouldCreateCycle(String courseCode, String fromKnowledgePointId,
                                    String toKnowledgePointId, String relationType) {
        if (!"hierarchy".equals(relationType) && !"prerequisite".equals(relationType)) {
            return false;
        }

        Map<String, Set<String>> outgoing = new HashMap<>();
        for (KnowledgeRelation relation : listByCourseCode(courseCode)) {
            if (!relationType.equals(relation.getRelationType())) continue;
            outgoing.computeIfAbsent(relation.getFromKnowledgePointId(), ignored -> new HashSet<>())
                    .add(relation.getToKnowledgePointId());
        }

        ArrayDeque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(toKnowledgePointId);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!visited.add(current)) continue;
            if (fromKnowledgePointId.equals(current)) return true;
            queue.addAll(outgoing.getOrDefault(current, Set.of()));
        }
        return false;
    }

    private void validateRelation(String courseCode, KnowledgeRelation relation) {
        if (relation == null) throw new IllegalArgumentException("知识关系不能为空");
        KnowledgePoint from = knowledgePointService.getById(relation.getFromKnowledgePointId());
        KnowledgePoint to = knowledgePointService.getById(relation.getToKnowledgePointId());
        if (from == null || to == null) throw new IllegalArgumentException("知识点不存在");
        if (!courseCode.equals(from.getCourseCode()) || !courseCode.equals(to.getCourseCode())) {
            throw new IllegalArgumentException("知识点不属于当前课程");
        }
        if (relation.getFromKnowledgePointId().equals(relation.getToKnowledgePointId())) {
            throw new IllegalArgumentException("不能连接同一个知识点");
        }
    }
}

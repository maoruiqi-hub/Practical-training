package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.KnowledgeRelation;
import com.neu.CoursePlatform.mapper.KnowledgeRelationMapper;
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
}

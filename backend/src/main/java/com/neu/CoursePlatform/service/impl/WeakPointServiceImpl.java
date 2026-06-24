package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.CoursePlatform.dto.WeakKnowledgePointDTO;
import com.neu.CoursePlatform.entity.KnowledgeMastery;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.service.KnowledgeMasteryService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.WeakPointService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WeakPointServiceImpl implements WeakPointService {
    private final KnowledgeMasteryService masteryService;
    private final KnowledgePointService pointService;

    public WeakPointServiceImpl(KnowledgeMasteryService masteryService, KnowledgePointService pointService) {
        this.masteryService = masteryService;
        this.pointService = pointService;
    }

    @Override
    public List<WeakKnowledgePointDTO> listByCourseCode(String courseCode) {
        Map<String, List<KnowledgeMastery>> groups = groupByKnowledgePoint(courseCode);
        List<WeakKnowledgePointDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<KnowledgeMastery>> entry : groups.entrySet()) {
            KnowledgePoint point = pointService.getById(entry.getKey());
            if (point == null) continue;
            double average = entry.getValue().stream()
                    .mapToInt(KnowledgeMastery::getMasteryScore)
                    .average()
                    .orElse(0);
            result.add(new WeakKnowledgePointDTO(point.getKnowledgePointId(), point.getName(), average,
                    entry.getValue().size(), "knowledge_mastery"));
        }

        result.sort(Comparator.comparingDouble(WeakKnowledgePointDTO::getAverageMasteryScore)
                .thenComparing(WeakKnowledgePointDTO::getStudentCount, Comparator.reverseOrder()));
        return result;
    }

    private Map<String, List<KnowledgeMastery>> groupByKnowledgePoint(String courseCode) {
        List<KnowledgeMastery> masteries = masteryService.list(
                new LambdaQueryWrapper<KnowledgeMastery>().eq(KnowledgeMastery::getCourseCode, courseCode));
        Map<String, List<KnowledgeMastery>> groups = new HashMap<>();
        for (KnowledgeMastery mastery : masteries) {
            groups.computeIfAbsent(mastery.getKnowledgePointId(), ignored -> new ArrayList<>()).add(mastery);
        }
        return groups;
    }
}

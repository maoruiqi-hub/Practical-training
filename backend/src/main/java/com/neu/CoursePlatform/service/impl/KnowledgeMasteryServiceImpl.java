package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.dto.KnowledgeMasteryUpdateRequest;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgeMastery;
import com.neu.CoursePlatform.mapper.KnowledgeMasteryMapper;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.KnowledgeMasteryService;
import com.neu.CoursePlatform.service.StudentService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgeMasteryServiceImpl extends ServiceImpl<KnowledgeMasteryMapper, KnowledgeMastery>
        implements KnowledgeMasteryService {

    private final KnowledgePointService knowledgePointService;
    private final StudentService studentService;

    public KnowledgeMasteryServiceImpl(KnowledgePointService knowledgePointService,
                                       StudentService studentService) {
        this.knowledgePointService = knowledgePointService;
        this.studentService = studentService;
    }

    @Override
    @Transactional
    public KnowledgeMastery upsert(KnowledgeMasteryUpdateRequest request) {
        String validationMessage = validateForUpsert(request);
        if (validationMessage != null) {
            throw new IllegalArgumentException(validationMessage);
        }
        KnowledgeMastery mastery = getOne(new LambdaQueryWrapper<KnowledgeMastery>()
                .eq(KnowledgeMastery::getStudentNo, request.getStudentNo())
                .eq(KnowledgeMastery::getCourseCode, request.getCourseCode())
                .eq(KnowledgeMastery::getKnowledgePointId, request.getKnowledgePointId()));

        if (mastery == null) {
            mastery = new KnowledgeMastery();
            mastery.setStudentNo(request.getStudentNo());
            mastery.setCourseCode(request.getCourseCode());
            mastery.setKnowledgePointId(request.getKnowledgePointId());
        }
        mastery.setMasteryScore(request.getMasteryScore());
        mastery.setSourceType(request.getSourceType());
        mastery.setSourceId(request.getSourceId());
        mastery.setUpdatedAt(LocalDateTime.now());
        saveOrUpdate(mastery);
        return mastery;
    }

    @Override
    public List<KnowledgeMastery> listByStudentAndCourse(String studentNo, String courseCode) {
        return list(new LambdaQueryWrapper<KnowledgeMastery>()
                .eq(KnowledgeMastery::getStudentNo, studentNo)
                .eq(KnowledgeMastery::getCourseCode, courseCode)
                .orderByAsc(KnowledgeMastery::getKnowledgePointId));
    }

    @Override
    public int removeByKnowledgePoint(String knowledgePointId) {
        boolean removed = remove(new LambdaQueryWrapper<KnowledgeMastery>()
                .eq(KnowledgeMastery::getKnowledgePointId, knowledgePointId));
        return removed ? 1 : 0;
    }

    @EventListener
    public void handleAssessmentResult(GameEvent event) {
        if (event == null || event.getPayload() == null) return;
        if (!GameEventTypes.ANSWER_CORRECT.equals(event.getEventType())
                && !GameEventTypes.ANSWER_WRONG.equals(event.getEventType())) {
            return;
        }
        Object knowledgePointId = event.getPayload().get("knowledge_point_id");
        if (knowledgePointId == null || knowledgePointId.toString().isBlank()) return;
        KnowledgeMasteryUpdateRequest request = new KnowledgeMasteryUpdateRequest();
        request.setStudentNo(event.getStudentId());
        request.setCourseCode(event.getCourseId());
        request.setKnowledgePointId(knowledgePointId.toString());
        request.setMasteryScore(GameEventTypes.ANSWER_CORRECT.equals(event.getEventType()) ? 100 : 0);
        request.setSourceType("assessment");
        request.setSourceId(event.getSourceId());
        upsert(request);
    }

    @Override
    public String validateForUpsert(KnowledgeMasteryUpdateRequest request) {
        if (request == null || isBlank(request.getStudentNo()) || isBlank(request.getCourseCode())
                || isBlank(request.getKnowledgePointId()) || request.getMasteryScore() == null
                || request.getMasteryScore() < 0 || request.getMasteryScore() > 100) {
            return "掌握度请求不合法";
        }
        if (studentService.getById(request.getStudentNo()) == null) {
            return "学生不存在";
        }
        KnowledgePoint point = knowledgePointService.getById(request.getKnowledgePointId());
        if (point == null || !request.getCourseCode().equals(point.getCourseCode())) {
            return "知识点不属于当前课程";
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

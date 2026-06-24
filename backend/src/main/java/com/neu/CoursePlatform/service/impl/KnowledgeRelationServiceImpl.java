package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgeRelation;
import com.neu.CoursePlatform.mapper.KnowledgeRelationMapper;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.KnowledgeRelationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeRelationServiceImpl extends ServiceImpl<KnowledgeRelationMapper, KnowledgeRelation> implements KnowledgeRelationService {

    private final KnowledgePointService knowledgePointService;

    public KnowledgeRelationServiceImpl(KnowledgePointService knowledgePointService) {
        this.knowledgePointService = knowledgePointService;
    }

    @Override
    public List<KnowledgeRelation> listByCourse(String courseCode) {
        return baseMapper.selectByCourse(courseCode);
    }

    @Override
    public void createRelation(String courseCode, KnowledgeRelation relation) {
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

        relation.setCourseCode(courseCode);
        if (relation.getRelationType() == null || relation.getRelationType().isBlank()) {
            relation.setRelationType("prerequisite");
        }
        save(relation);
    }
}

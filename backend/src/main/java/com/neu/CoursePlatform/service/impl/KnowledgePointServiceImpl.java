package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgeRelation;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.mapper.KnowledgePointMapper;
import com.neu.CoursePlatform.mapper.KnowledgeRelationMapper;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.QuestionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgePointServiceImpl extends ServiceImpl<KnowledgePointMapper, KnowledgePoint> implements KnowledgePointService {

    private final QuestionService questionService;
    private final KnowledgeRelationMapper relationMapper;

    public KnowledgePointServiceImpl(QuestionService questionService, KnowledgeRelationMapper relationMapper) {
        this.questionService = questionService;
        this.relationMapper = relationMapper;
    }

    @Override
    public List<KnowledgePoint> listByCourse(String courseCode, String lessonNo, String keyword) {
        return baseMapper.selectByCourse(courseCode, lessonNo, keyword);
    }

    @Override
    public void removePoint(String pointId) {
        long questionCount = questionService.count(new QueryWrapper<Question>().eq("knowledge_point_id", pointId));
        if (questionCount > 0) {
            throw new IllegalArgumentException("该知识点已被题目引用，请先调整相关题目");
        }
        long relationCount = relationMapper.selectCount(new QueryWrapper<KnowledgeRelation>()
                .eq("from_knowledge_point_id", pointId)
                .or()
                .eq("to_knowledge_point_id", pointId));
        if (relationCount > 0) {
            throw new IllegalArgumentException("该知识点已被知识关系引用，请先删除相关关系");
        }
        removeById(pointId);
    }
}

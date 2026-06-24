package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.KnowledgeEdge;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.mapper.KnowledgePointMapper;
import com.neu.CoursePlatform.service.KnowledgeEdgeService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.QuestionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgePointServiceImpl extends ServiceImpl<KnowledgePointMapper, KnowledgePoint> implements KnowledgePointService {

    private final QuestionService questionService;
    private final KnowledgeEdgeService edgeService;

    public KnowledgePointServiceImpl(QuestionService questionService, KnowledgeEdgeService edgeService) {
        this.questionService = questionService;
        this.edgeService = edgeService;
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
        long edgeCount = edgeService.count(new QueryWrapper<KnowledgeEdge>()
                .eq("source_id", pointId)
                .or()
                .eq("target_id", pointId));
        if (edgeCount > 0) {
            throw new IllegalArgumentException("该知识点已被知识关系引用，请先删除相关关系");
        }
        removeById(pointId);
    }
}

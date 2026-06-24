package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.KnowledgeEdge;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.mapper.KnowledgeEdgeMapper;
import com.neu.CoursePlatform.service.KnowledgeEdgeService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeEdgeServiceImpl extends ServiceImpl<KnowledgeEdgeMapper, KnowledgeEdge> implements KnowledgeEdgeService {

    private final KnowledgePointService knowledgePointService;

    public KnowledgeEdgeServiceImpl(KnowledgePointService knowledgePointService) {
        this.knowledgePointService = knowledgePointService;
    }

    @Override
    public List<KnowledgeEdge> listByCourse(String courseCode) {
        return baseMapper.selectByCourse(courseCode);
    }

    @Override
    public void createEdge(String courseCode, KnowledgeEdge edge) {
        if (edge == null) throw new IllegalArgumentException("知识关系不能为空");
        KnowledgePoint source = knowledgePointService.getById(edge.getSourceId());
        KnowledgePoint target = knowledgePointService.getById(edge.getTargetId());
        if (source == null || target == null) throw new IllegalArgumentException("知识点不存在");
        if (!courseCode.equals(source.getCourseCode()) || !courseCode.equals(target.getCourseCode())) {
            throw new IllegalArgumentException("知识点不属于当前课程");
        }
        if (edge.getSourceId().equals(edge.getTargetId())) throw new IllegalArgumentException("不能连接同一个知识点");

        edge.setCourseCode(courseCode);
        if (edge.getRelationType() == null || edge.getRelationType().isBlank()) {
            edge.setRelationType("prerequisite");
        }
        save(edge);
    }
}

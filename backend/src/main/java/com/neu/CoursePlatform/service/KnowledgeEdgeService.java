package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.KnowledgeEdge;

import java.util.List;

public interface KnowledgeEdgeService extends IService<KnowledgeEdge> {

    List<KnowledgeEdge> listByCourse(String courseCode);

    void createEdge(String courseCode, KnowledgeEdge edge);
}

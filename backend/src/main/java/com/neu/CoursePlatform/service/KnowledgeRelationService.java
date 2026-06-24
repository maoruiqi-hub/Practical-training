package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.KnowledgeRelation;

import java.util.List;

public interface KnowledgeRelationService extends IService<KnowledgeRelation> {

    List<KnowledgeRelation> listByCourse(String courseCode);

    void createRelation(String courseCode, KnowledgeRelation relation);
}

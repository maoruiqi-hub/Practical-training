package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.KnowledgeRelation;

import java.util.List;

public interface KnowledgeRelationService extends IService<KnowledgeRelation> {

    List<KnowledgeRelation> listByCourseCode(String courseCode);

    boolean relationExists(String courseCode, String fromKnowledgePointId,
                           String toKnowledgePointId, String relationType);

    boolean wouldCreateCycle(String courseCode, String fromKnowledgePointId,
                             String toKnowledgePointId, String relationType);
}

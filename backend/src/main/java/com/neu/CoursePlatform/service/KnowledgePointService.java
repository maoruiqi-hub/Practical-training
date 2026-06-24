package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.KnowledgePoint;

import java.util.List;

public interface KnowledgePointService extends IService<KnowledgePoint> {

    List<KnowledgePoint> listByCourseCode(String courseCode, String chapter);

    /**
     * Deletes a knowledge point and clears every Module 1 relationship that
     * refers to it. Resources are preserved, but become uncategorised.
     */
    boolean deleteWithDependencies(String knowledgePointId);
}

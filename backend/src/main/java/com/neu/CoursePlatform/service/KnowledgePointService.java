package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.KnowledgePoint;

import java.util.List;

public interface KnowledgePointService extends IService<KnowledgePoint> {

    List<KnowledgePoint> listByCourse(String courseCode, String lessonNo, String keyword);

    void removePoint(String pointId);

    List<KnowledgePoint> listByCourseCode(String courseCode, String chapter);

    /** In-process contract used by Module 4 to calculate tower accessibility and DEF. */
    List<KnowledgePoint> getPrerequisiteChain(String knowledgePointId);

    /**
     * Deletes a knowledge point and clears every Module 1 relationship that
     * refers to it. Resources are preserved, but become uncategorised.
     */
    boolean deleteWithDependencies(String knowledgePointId);
}

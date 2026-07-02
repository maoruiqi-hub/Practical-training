package com.neu.CoursePlatform.service;

/** 模块三判分后调用，模块一负责决定并发布楼层通关事件。 */
public interface FloorProgressService {
    void recordQuizResult(String studentId, String courseId, String knowledgePointId,
                          String sourceId, boolean correct, int maxScore);
    boolean updateFloorStatus(String studentId, String courseId, String knowledgePointId, String status);
}

package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.entity.KnowledgePointFloorStatus;

import java.util.List;

public interface KnowledgePointFloorStatusService {
    KnowledgePointFloorStatus updateStatus(String studentNo, String courseCode, String knowledgePointId, String status);
    List<KnowledgePointFloorStatus> listByStudentAndCourse(String studentNo, String courseCode);
}

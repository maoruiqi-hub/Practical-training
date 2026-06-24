package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.KnowledgePoint;

import java.util.List;

public interface KnowledgePointService extends IService<KnowledgePoint> {

    List<KnowledgePoint> listByCourse(String courseCode, String lessonNo, String keyword);

    void removePoint(String pointId);
}

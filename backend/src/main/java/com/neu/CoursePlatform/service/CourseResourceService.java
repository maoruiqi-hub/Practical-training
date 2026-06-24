package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.CourseResource;

import java.util.List;

public interface CourseResourceService extends IService<CourseResource> {

    List<CourseResource> listByFilters(String courseCode, String chapter,
                                       String knowledgePointId, String resourceType);
}

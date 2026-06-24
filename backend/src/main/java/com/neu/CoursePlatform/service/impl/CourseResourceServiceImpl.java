package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.CourseResource;
import com.neu.CoursePlatform.mapper.CourseResourceMapper;
import com.neu.CoursePlatform.service.CourseResourceService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseResourceServiceImpl extends ServiceImpl<CourseResourceMapper, CourseResource>
        implements CourseResourceService {

    @Override
    public List<CourseResource> listByFilters(String courseCode, String chapter,
                                              String knowledgePointId, String resourceType) {
        LambdaQueryWrapper<CourseResource> query = new LambdaQueryWrapper<CourseResource>()
                .eq(CourseResource::getCourseCode, courseCode)
                .orderByDesc(CourseResource::getUploadedAt)
                .orderByDesc(CourseResource::getResourceId);
        if (chapter != null && !chapter.isBlank()) query.eq(CourseResource::getChapter, chapter);
        if (knowledgePointId != null && !knowledgePointId.isBlank()) {
            query.eq(CourseResource::getKnowledgePointId, knowledgePointId);
        }
        if (resourceType != null && !resourceType.isBlank()) {
            query.eq(CourseResource::getResourceType, resourceType.trim().toLowerCase());
        }
        return list(query);
    }
}

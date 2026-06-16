package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.Lesson;
import com.neu.CoursePlatform.mapper.CourseMapper;
import com.neu.CoursePlatform.service.CourseService;
import com.neu.CoursePlatform.service.LessonService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final LessonService lessonService;

    public CourseServiceImpl(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @Override
    public List<Course> searchByKeyword(String keyword) {
        return baseMapper.selectByKeyword(keyword);
    }

    @Override
    public List<Lesson> listLessons(String courseCode) {
        return lessonService.listByCourseCode(courseCode);
    }
}

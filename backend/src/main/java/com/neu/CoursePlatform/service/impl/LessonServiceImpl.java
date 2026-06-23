package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.dto.LessonDTO;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.Lesson;
import com.neu.CoursePlatform.mapper.CourseMapper;
import com.neu.CoursePlatform.mapper.LessonMapper;
import com.neu.CoursePlatform.service.LessonService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LessonServiceImpl extends ServiceImpl<LessonMapper, Lesson> implements LessonService {

    private final CourseMapper courseMapper;

    public LessonServiceImpl(CourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    @Override
    public List<Lesson> listByCourseCode(String courseCode) {
        return baseMapper.selectByCourseCode(courseCode);
    }

    @Override
    public LessonDTO getDetailDto(String lessonNo) {
        Lesson lesson = getById(lessonNo);
        if (lesson == null) return null;
        LessonDTO dto = new LessonDTO();
        dto.setLessonNo(lesson.getLessonNo());
        dto.setCourseCode(lesson.getCourseCode());
        dto.setLessonTitle(lesson.getLessonTitle());
        dto.setResourceType(lesson.getResourceType());
        dto.setResourceUrl(lesson.getResourceUrl());
        dto.setDescription(lesson.getDescription());
        Course course = courseMapper.selectById(lesson.getCourseCode());
        if (course != null) {
            dto.setCourseName(course.getCourseName());
            dto.setTeacherName(course.getTeacher());
        }
        return dto;
    }

    @Override
    public List<Lesson> searchByKeyword(String keyword) {
        return baseMapper.selectByKeyword(keyword);
    }
}

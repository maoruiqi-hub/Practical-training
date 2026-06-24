package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.dto.CourseDTO;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.Lesson;
import com.neu.CoursePlatform.mapper.CourseMapper;
import com.neu.CoursePlatform.service.CourseService;
import com.neu.CoursePlatform.service.LessonService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    public List<CourseDTO> searchDtoByKeyword(String keyword) {
        return toDtos(searchByKeyword(keyword));
    }

    @Override
    public List<CourseDTO> listDto() {
        return toDtos(list());
    }

    @Override
    public List<Lesson> listLessons(String courseCode) {
        return lessonService.listByCourseCode(courseCode);
    }

    private List<CourseDTO> toDtos(List<Course> courses) {
        List<CourseDTO> dtos = new ArrayList<>();
        for (Course course : courses) {
            dtos.add(toDto(course));
        }
        return dtos;
    }

    private CourseDTO toDto(Course course) {
        CourseDTO dto = new CourseDTO();
        dto.setCourseCode(course.getCourseCode());
        dto.setCourseName(course.getCourseName());
        dto.setTeacher(course.getTeacher());
        dto.setCredits(course.getCredits());
        dto.setHours(course.getHours());
        dto.setCoverUrl(course.getCoverUrl());
        dto.setDescription(course.getDescription());
        dto.setApplicableMajor(course.getApplicableMajor());
        dto.setCourseObjectives(course.getCourseObjectives());
        dto.setLessonCount(lessonService.listByCourseCode(course.getCourseCode()).size());
        return dto;
    }
}

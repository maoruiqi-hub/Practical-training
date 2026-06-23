package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.dto.CourseDTO;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.Lesson;

import java.util.List;

public interface CourseService extends IService<Course> {

    List<Course> searchByKeyword(String keyword);

    List<CourseDTO> searchDtoByKeyword(String keyword);

    List<CourseDTO> listDto();

    List<Lesson> listLessons(String courseCode);
}

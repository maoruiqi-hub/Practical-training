package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.Lesson;

import java.util.List;

public interface LessonService extends IService<Lesson> {

    List<Lesson> listByCourseCode(String courseCode);

    List<Lesson> searchByKeyword(String keyword);
}

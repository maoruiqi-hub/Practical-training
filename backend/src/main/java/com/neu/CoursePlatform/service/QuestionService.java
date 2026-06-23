package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.Question;

import java.util.List;

public interface QuestionService extends IService<Question> {

    List<Question> listByCourseCode(String courseCode);

    List<Question> listByLessonNo(String lessonNo);

    List<Question> searchByKeyword(String keyword);
}

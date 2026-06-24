package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.dto.ExamGenerateRequest;
import com.neu.CoursePlatform.entity.Question;

import java.util.List;

public interface QuestionService extends IService<Question> {

    List<Question> listByCourseCode(String courseCode);

    List<Question> listByLessonNo(String lessonNo);

    List<Question> searchByKeyword(String keyword);

    List<Question> filterQuestions(String courseCode, String lessonNo, String knowledgePointId,
                                   String type, Integer difficulty, String keyword);

    List<Question> generateExam(String courseCode, ExamGenerateRequest request);
}

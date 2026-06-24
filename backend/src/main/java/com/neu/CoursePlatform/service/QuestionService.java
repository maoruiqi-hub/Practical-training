package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.dto.PaperGenerateRequest;
import com.neu.CoursePlatform.entity.Question;

import java.util.List;

public interface QuestionService extends IService<Question> {

    List<Question> listByCourseCode(String courseCode);

    List<Question> listByLessonNo(String lessonNo);

    List<Question> searchByKeyword(String keyword);

    List<Question> filterQuestions(String courseCode, String lessonNo, String knowledgePointId,
                                   String type, Integer difficulty, String keyword);

    List<Question> generatePaper(String courseCode, PaperGenerateRequest request);
}

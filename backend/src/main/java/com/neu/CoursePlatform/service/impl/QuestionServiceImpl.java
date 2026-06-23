package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.mapper.QuestionMapper;
import com.neu.CoursePlatform.service.QuestionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Override
    public List<Question> listByCourseCode(String courseCode) {
        return baseMapper.selectByCourseCode(courseCode);
    }

    @Override
    public List<Question> listByLessonNo(String lessonNo) {
        return baseMapper.selectByLessonNo(lessonNo);
    }

    @Override
    public List<Question> searchByKeyword(String keyword) {
        return baseMapper.selectByKeyword(keyword);
    }
}

package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.ExamQuestion;
import com.neu.CoursePlatform.mapper.ExamQuestionMapper;
import com.neu.CoursePlatform.service.ExamQuestionService;
import org.springframework.stereotype.Service;

@Service
public class ExamQuestionServiceImpl extends ServiceImpl<ExamQuestionMapper, ExamQuestion> implements ExamQuestionService {
}

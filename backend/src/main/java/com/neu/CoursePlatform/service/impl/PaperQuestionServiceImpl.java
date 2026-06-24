package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.PaperQuestion;
import com.neu.CoursePlatform.mapper.PaperQuestionMapper;
import com.neu.CoursePlatform.service.PaperQuestionService;
import org.springframework.stereotype.Service;

@Service
public class PaperQuestionServiceImpl extends ServiceImpl<PaperQuestionMapper, PaperQuestion> implements PaperQuestionService {
}

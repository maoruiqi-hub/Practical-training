package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.Lesson;
import com.neu.CoursePlatform.mapper.LessonMapper;
import com.neu.CoursePlatform.service.LessonService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LessonServiceImpl extends ServiceImpl<LessonMapper, Lesson> implements LessonService {

    @Override
    public List<Lesson> listByCourseCode(String courseCode) {
        return baseMapper.selectByCourseCode(courseCode);
    }

    @Override
    public List<Lesson> searchByKeyword(String keyword) {
        return baseMapper.selectByKeyword(keyword);
    }
}

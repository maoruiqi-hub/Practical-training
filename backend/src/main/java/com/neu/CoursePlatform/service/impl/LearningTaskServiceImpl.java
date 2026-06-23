package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.mapper.LearningTaskMapper;
import com.neu.CoursePlatform.service.LearningTaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class LearningTaskServiceImpl extends ServiceImpl<LearningTaskMapper, LearningTask> implements LearningTaskService {

    @Override
    public List<LearningTask> listByCourseCode(String courseCode) {
        return baseMapper.selectByCourseCode(courseCode);
    }

    @Override
    public List<LearningTask> searchByKeyword(String keyword) {
        return baseMapper.selectByKeyword(keyword);
    }

    @Override
    public void applyDeadline(LearningTask task, String deadline) {
        if (deadline == null || deadline.isEmpty()) {
            task.setDeadline(null);
            return;
        }
        try {
            task.setDeadline(LocalDateTime.parse(deadline.replace(" ", "T")));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("截止时间格式错误，请使用 yyyy-MM-dd HH:mm:ss");
        }
    }
}

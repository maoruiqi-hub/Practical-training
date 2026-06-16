package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.LearningTask;

import java.util.List;

public interface LearningTaskService extends IService<LearningTask> {

    List<LearningTask> listByCourseCode(String courseCode);

    List<LearningTask> searchByKeyword(String keyword);
}

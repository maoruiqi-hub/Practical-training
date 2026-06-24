package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.LearningTask;

import java.util.List;

public interface LearningTaskService extends IService<LearningTask> {

    String ONLINE_QUIZ_TYPE = "在线测验";

    List<LearningTask> listByCourseCode(String courseCode);

    List<LearningTask> searchByKeyword(String keyword);

    void applyDeadline(LearningTask task, String deadline);

    default boolean isQuizTask(LearningTask task) {
        return task != null && (ONLINE_QUIZ_TYPE.equals(task.getTaskType())
                || "boss".equalsIgnoreCase(task.getTaskType())
                || "boss_exam".equalsIgnoreCase(task.getTaskType()));
    }
}

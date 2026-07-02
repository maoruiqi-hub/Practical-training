package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.LearningTask;

import java.util.List;
import java.util.Map;

public interface LearningTaskService extends IService<LearningTask> {

    String ONLINE_QUIZ_TYPE = "在线测验";

    List<LearningTask> listByCourseCode(String courseCode);

    List<LearningTask> searchByKeyword(String keyword);

    List<LearningTask> listFiltered(Map<String, String> filters);

    void applyDeadline(LearningTask task, String deadline);

    /** 检查任务下是否有提交记录 */
    boolean hasSubmissions(String taskNo);

    /** 判断是否为测验类型任务（含Boss层） */
    default boolean isQuizTask(LearningTask task) {
        return task != null && (ONLINE_QUIZ_TYPE.equals(task.getTaskType())
                || "boss".equalsIgnoreCase(task.getTaskType())
                || "boss_exam".equalsIgnoreCase(task.getTaskType()));
    }
}

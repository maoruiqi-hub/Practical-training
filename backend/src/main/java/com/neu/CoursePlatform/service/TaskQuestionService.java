package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.TaskQuestion;

import java.util.List;

public interface TaskQuestionService extends IService<TaskQuestion> {

    List<TaskQuestion> listByTaskNo(String taskNo);

    void addQuestionsToTask(String taskNo, List<String> questionIds);

    boolean deleteByTaskAndQuestion(String taskNo, String questionId);

    boolean removeQuestionFromTask(String taskNo, String questionId);
}

package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.entity.TaskQuestion;
import com.neu.CoursePlatform.mapper.TaskQuestionMapper;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.QuestionService;
import com.neu.CoursePlatform.service.TaskQuestionService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class TaskQuestionServiceImpl extends ServiceImpl<TaskQuestionMapper, TaskQuestion> implements TaskQuestionService {

    private final LearningTaskService taskService;
    private final QuestionService questionService;

    public TaskQuestionServiceImpl(LearningTaskService taskService, QuestionService questionService) {
        this.taskService = taskService;
        this.questionService = questionService;
    }

    @Override
    public List<TaskQuestion> listByTaskNo(String taskNo) {
        return baseMapper.selectByTaskNo(taskNo);
    }

    @Override
    public void addQuestionsToTask(String taskNo, List<String> questionIds) {
        LearningTask task = taskService.getById(taskNo);
        if (task == null) throw new IllegalArgumentException("任务不存在");
        if (questionIds == null || questionIds.isEmpty()) throw new IllegalArgumentException("请选择题目");

        Set<String> uniqueQuestionIds = new LinkedHashSet<>(questionIds);
        for (String questionId : uniqueQuestionIds) {
            validateQuestionBelongsToTask(task, questionId);
        }
        for (String questionId : uniqueQuestionIds) {
            if (baseMapper.selectCount(new QueryWrapper<TaskQuestion>()
                    .eq("task_no", taskNo).eq("question_id", questionId)) > 0) continue;
            TaskQuestion taskQuestion = new TaskQuestion();
            taskQuestion.setTaskNo(taskNo);
            taskQuestion.setQuestionId(questionId);
            save(taskQuestion);
        }
    }

    @Override
    public boolean deleteByTaskAndQuestion(String taskNo, String questionId) {
        return remove(new QueryWrapper<TaskQuestion>()
                .eq("task_no", taskNo).eq("question_id", questionId));
    }

    @Override
    public boolean removeQuestionFromTask(String taskNo, String questionId) {
        LearningTask task = taskService.getById(taskNo);
        if (task == null) throw new IllegalArgumentException("任务不存在");
        validateQuestionBelongsToTask(task, questionId);
        return deleteByTaskAndQuestion(taskNo, questionId);
    }

    private void validateQuestionBelongsToTask(LearningTask task, String questionId) {
        Question question = questionService.getById(questionId);
        if (question == null) throw new IllegalArgumentException("题目不存在：" + questionId);
        if (!task.getCourseCode().equals(question.getCourseCode())) {
            throw new IllegalArgumentException("题目不属于当前课程：" + questionId);
        }
    }
}

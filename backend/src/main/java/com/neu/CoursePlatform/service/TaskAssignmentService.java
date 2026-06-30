package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.TaskAssignment;

import java.util.List;

public interface TaskAssignmentService extends IService<TaskAssignment> {
    TaskAssignment assignTask(LearningTask task, String studentNo, String assignedBy, String note);

    List<LearningTask> listAssignedTasks(String studentNo, String courseCode, String taskType, String taskStatus, String lessonNo);

    TaskAssignment getActiveAssignment(String taskNo, String studentNo);

    long countActiveByTaskNo(String taskNo);

    void markSubmitted(String taskNo, String studentNo);

    void markCompleted(String taskNo, String studentNo);

    void cancelByTaskNo(String taskNo);
}

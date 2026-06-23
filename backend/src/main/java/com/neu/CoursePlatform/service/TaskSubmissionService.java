package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.dto.TaskSubmissionDTO;
import com.neu.CoursePlatform.entity.TaskSubmission;

import java.util.List;
import java.util.Map;

public interface TaskSubmissionService extends IService<TaskSubmission> {

    List<TaskSubmission> listByStudentNo(String studentNo);

    List<TaskSubmission> listByTaskNo(String taskNo);

    List<TaskSubmissionDTO> listDtoByTaskNo(String taskNo);

    String getTaskCourseCode(String taskNo);

    boolean isTaskOverdue(String taskNo);

    boolean hasSubmitted(String taskNo, String studentNo);

    Map<String, Object> buildGradeDetail(String submissionId);

    void applyInitialGrading(TaskSubmission sub);

    int autoScoreChoices(TaskSubmission sub);
}

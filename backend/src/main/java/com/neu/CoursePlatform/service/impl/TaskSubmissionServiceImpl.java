package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.dto.TaskSubmissionDTO;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.mapper.TaskSubmissionMapper;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.StudentService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskSubmissionServiceImpl extends ServiceImpl<TaskSubmissionMapper, TaskSubmission> implements TaskSubmissionService {

    private final LearningTaskService taskService;
    private final StudentService studentService;

    public TaskSubmissionServiceImpl(LearningTaskService taskService, StudentService studentService) {
        this.taskService = taskService;
        this.studentService = studentService;
    }

    @Override
    public List<TaskSubmission> listByStudentNo(String studentNo) {
        return baseMapper.selectByStudentNo(studentNo);
    }

    @Override
    public List<TaskSubmission> listByTaskNo(String taskNo) {
        return baseMapper.selectByTaskNo(taskNo);
    }

    @Override
    public List<TaskSubmissionDTO> listDtoByTaskNo(String taskNo) {
        LearningTask task = taskService.getById(taskNo);
        List<TaskSubmission> list = baseMapper.selectByTaskNo(taskNo);
        List<TaskSubmissionDTO> dtos = new ArrayList<>();
        for (TaskSubmission sub : list) {
            TaskSubmissionDTO dto = new TaskSubmissionDTO();
            dto.setSubmissionId(sub.getSubmissionId());
            dto.setTaskNo(sub.getTaskNo());
            dto.setTaskType(task != null ? task.getTaskType() : "");
            dto.setStudentNo(sub.getStudentNo());
            Student stu = studentService.getById(sub.getStudentNo());
            dto.setStudentName(stu != null ? stu.getName() : "");
            dto.setContent(sub.getContent());
            dto.setFilePath(sub.getFilePath());
            dto.setSubmitTime(sub.getSubmitTime());
            dto.setScore(sub.getScore());
            dto.setStatus(sub.getStatus());
            dto.setFeedback(sub.getFeedback());
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    public String getTaskCourseCode(String taskNo) {
        LearningTask task = taskService.getById(taskNo);
        return task != null ? task.getCourseCode() : null;
    }

    @Override
    public boolean isTaskOverdue(String taskNo) {
        LearningTask task = taskService.getById(taskNo);
        return task != null && task.getDeadline() != null
                && java.time.LocalDateTime.now().isAfter(task.getDeadline());
    }
}

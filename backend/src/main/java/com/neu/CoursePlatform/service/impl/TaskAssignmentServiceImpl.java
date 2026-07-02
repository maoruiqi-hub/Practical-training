package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.TaskAssignment;
import com.neu.CoursePlatform.mapper.TaskAssignmentMapper;
import com.neu.CoursePlatform.service.TaskAssignmentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskAssignmentServiceImpl extends ServiceImpl<TaskAssignmentMapper, TaskAssignment>
        implements TaskAssignmentService {

    @Override
    public TaskAssignment assignTask(LearningTask task, String studentNo, String assignedBy, String note) {
        TaskAssignment existing = getOne(new LambdaQueryWrapper<TaskAssignment>()
                .eq(TaskAssignment::getTaskNo, task.getTaskNo())
                .eq(TaskAssignment::getStudentNo, studentNo)
                .last("LIMIT 1"));
        if (existing != null) {
            existing.setCourseCode(task.getCourseCode());
            existing.setAssignedBy(assignedBy);
            existing.setAssignedAt(LocalDateTime.now());
            existing.setStatus("assigned");
            existing.setNote(note);
            updateById(existing);
            return existing;
        }

        TaskAssignment assignment = new TaskAssignment();
        assignment.setTaskNo(task.getTaskNo());
        assignment.setCourseCode(task.getCourseCode());
        assignment.setStudentNo(studentNo);
        assignment.setAssignedBy(assignedBy);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setStatus("assigned");
        assignment.setNote(note);
        save(assignment);
        return assignment;
    }

    @Override
    public List<LearningTask> listAssignedTasks(String studentNo, String courseCode, String taskType, String taskStatus, String lessonNo) {
        return baseMapper.selectAssignedTasks(studentNo, courseCode, taskType, taskStatus, lessonNo);
    }

    @Override
    public TaskAssignment getActiveAssignment(String taskNo, String studentNo) {
        return baseMapper.selectActiveByTaskAndStudent(taskNo, studentNo);
    }

    @Override
    public long countActiveByTaskNo(String taskNo) {
        Long count = baseMapper.countActiveByTaskNo(taskNo);
        return count == null ? 0 : count;
    }

    @Override
    public void markSubmitted(String taskNo, String studentNo) {
        update(new LambdaUpdateWrapper<TaskAssignment>()
                .eq(TaskAssignment::getTaskNo, taskNo)
                .eq(TaskAssignment::getStudentNo, studentNo)
                .ne(TaskAssignment::getStatus, "cancelled")
                .set(TaskAssignment::getStatus, "submitted"));
    }

    @Override
    public void markCompleted(String taskNo, String studentNo) {
        update(new LambdaUpdateWrapper<TaskAssignment>()
                .eq(TaskAssignment::getTaskNo, taskNo)
                .eq(TaskAssignment::getStudentNo, studentNo)
                .ne(TaskAssignment::getStatus, "cancelled")
                .set(TaskAssignment::getStatus, "completed"));
    }

    @Override
    public void cancelByTaskNo(String taskNo) {
        update(new LambdaUpdateWrapper<TaskAssignment>()
                .eq(TaskAssignment::getTaskNo, taskNo)
                .ne(TaskAssignment::getStatus, "cancelled")
                .set(TaskAssignment::getStatus, "cancelled"));
    }
}

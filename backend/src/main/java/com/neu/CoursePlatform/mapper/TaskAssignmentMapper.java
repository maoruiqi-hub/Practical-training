package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.TaskAssignment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TaskAssignmentMapper extends BaseMapper<TaskAssignment> {
    List<LearningTask> selectAssignedTasks(@Param("studentNo") String studentNo,
                                           @Param("courseCode") String courseCode,
                                           @Param("taskType") String taskType,
                                           @Param("taskStatus") String taskStatus,
                                           @Param("lessonNo") String lessonNo);

    TaskAssignment selectActiveByTaskAndStudent(@Param("taskNo") String taskNo,
                                                @Param("studentNo") String studentNo);

    Long countActiveByTaskNo(@Param("taskNo") String taskNo);
}

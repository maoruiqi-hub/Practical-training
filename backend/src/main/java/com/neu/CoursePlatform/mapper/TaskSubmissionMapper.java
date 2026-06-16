package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.TaskSubmission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务提交记录 Mapper
 */
public interface TaskSubmissionMapper extends BaseMapper<TaskSubmission> {

    /** 按学生查提交记录 */
    List<TaskSubmission> selectByStudentNo(@Param("studentNo") String studentNo);

    /** 按任务查提交记录 */
    List<TaskSubmission> selectByTaskNo(@Param("taskNo") String taskNo);
}

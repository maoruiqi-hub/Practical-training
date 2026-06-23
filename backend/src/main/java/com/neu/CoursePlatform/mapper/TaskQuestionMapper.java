package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.TaskQuestion;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 测验-题目关联 Mapper
 */
public interface TaskQuestionMapper extends BaseMapper<TaskQuestion> {

    /** 查某测验包含的所有题目关联 */
    List<TaskQuestion> selectByTaskNo(@Param("taskNo") String taskNo);
}

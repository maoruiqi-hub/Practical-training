package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.LearningTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学习任务 Mapper
 */
public interface LearningTaskMapper extends BaseMapper<LearningTask> {

    List<LearningTask> selectByCourseCode(@Param("courseCode") String courseCode);

    List<LearningTask> selectByKeyword(@Param("keyword") String keyword);
}

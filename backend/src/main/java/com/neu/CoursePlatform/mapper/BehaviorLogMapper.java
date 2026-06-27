package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.LearningBehaviorLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学习行为日志 Mapper
 */
public interface BehaviorLogMapper extends BaseMapper<LearningBehaviorLog> {

    List<LearningBehaviorLog> selectByUserId(@Param("userId") String userId);

    List<LearningBehaviorLog> selectByTaskNo(@Param("taskNo") String taskNo);

    List<LearningBehaviorLog> selectByFilters(@Param("userId") String userId,
                                               @Param("userType") String userType,
                                               @Param("actionType") String actionType,
                                               @Param("resourceType") String resourceType,
                                               @Param("courseId") String courseId,
                                               @Param("startTime") String startTime,
                                               @Param("endTime") String endTime);
}

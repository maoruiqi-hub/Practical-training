package com.neu.CoursePlatform.module5_analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.module5_analytics.entity.RiskAlert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 风险预警 Mapper
 */
public interface RiskAlertMapper extends BaseMapper<RiskAlert> {

    /** 查询某学生当前未解除预警 */
    List<RiskAlert> selectActiveByStudent(@Param("studentId") String studentId);

    /** 查询班级未解除预警（按等级和时间排序） */
    List<RiskAlert> selectActiveByClass(@Param("classId") String classId,
                                         @Param("studentIds") List<String> studentIds);

    /** 检查是否存在同类型活跃预警（去重用） */
    int countActiveByType(@Param("studentId") String studentId,
                          @Param("riskType") String riskType);

    /** 按班级ID和学生ID列表查询风险状态 */
    List<RiskAlert> selectActiveByStudentIds(@Param("studentIds") List<String> studentIds);
}

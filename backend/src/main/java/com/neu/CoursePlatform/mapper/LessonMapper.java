package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.Lesson;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 课时 Mapper
 */
public interface LessonMapper extends BaseMapper<Lesson> {

    /**
     * 根据课程编号查询课时列表
     */
    List<Lesson> selectByCourseCode(@Param("courseCode") String courseCode);

    List<Lesson> selectByKeyword(@Param("keyword") String keyword);
}

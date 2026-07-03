package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.dto.CourseDTO;
import com.neu.CoursePlatform.entity.Course;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 课程 Mapper
 */
public interface CourseMapper extends BaseMapper<Course> {

    List<Course> selectByKeyword(@Param("keyword") String keyword);

    List<CourseDTO> selectDtoList();

    List<CourseDTO> selectDtoByKeyword(@Param("keyword") String keyword);
}

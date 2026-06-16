package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.Teacher;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 教师 Mapper
 */
public interface TeacherMapper extends BaseMapper<Teacher> {

    Teacher selectByUsername(@Param("username") String username);

    List<Teacher> selectByKeyword(@Param("keyword") String keyword);
}

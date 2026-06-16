package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.Student;
import org.apache.ibatis.annotations.Param;

/**
 * 学生 Mapper
 */
public interface StudentMapper extends BaseMapper<Student> {

    Student selectByUsername(@Param("username") String username);
}

package com.neu.CoursePlatform.profile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.profile.entity.StudentProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StudentProfileMapper extends BaseMapper<StudentProfile> {
    @Select("""
            SELECT * FROM student_profile
            WHERE student_no = #{studentNo} AND course_code = #{courseCode}
            FOR UPDATE
            """)
    StudentProfile lockForUpdate(@Param("studentNo") Integer studentNo,
                                 @Param("courseCode") Integer courseCode);
}

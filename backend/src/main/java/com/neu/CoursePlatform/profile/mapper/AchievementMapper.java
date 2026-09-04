package com.neu.CoursePlatform.profile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.profile.entity.Achievement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AchievementMapper extends BaseMapper<Achievement> {
    @Select("SELECT student_no FROM student WHERE student_no = #{studentNo} FOR UPDATE")
    String lockStudentForBadgeEvaluation(@Param("studentNo") String studentNo);
}

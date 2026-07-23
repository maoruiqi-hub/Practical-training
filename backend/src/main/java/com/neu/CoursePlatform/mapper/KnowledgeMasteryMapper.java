package com.neu.CoursePlatform.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.KnowledgeMastery;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface KnowledgeMasteryMapper extends BaseMapper<KnowledgeMastery> {
    @Select("SELECT student_no FROM student WHERE student_no = #{studentNo} FOR UPDATE")
    String lockStudentForMasteryUpdate(@Param("studentNo") String studentNo);
}

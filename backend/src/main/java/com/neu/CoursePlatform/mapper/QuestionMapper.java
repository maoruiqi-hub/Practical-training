package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.Question;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 题库 Mapper
 */
public interface QuestionMapper extends BaseMapper<Question> {

    /** 按课程查 */
    List<Question> selectByCourseCode(@Param("courseCode") String courseCode);

    /** 按课时查 */
    List<Question> selectByLessonNo(@Param("lessonNo") String lessonNo);

    /** 模糊搜索题干/知识点 */
    List<Question> selectByKeyword(@Param("keyword") String keyword);
}

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

    /** 组合筛选题目 */
    List<Question> selectByFilter(@Param("courseCode") String courseCode,
                                  @Param("lessonNo") String lessonNo,
                                  @Param("knowledgePointId") String knowledgePointId,
                                  @Param("type") String type,
                                  @Param("difficulty") Integer difficulty,
                                  @Param("keyword") String keyword);
}

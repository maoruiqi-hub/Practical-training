package com.neu.CoursePlatform.module5_analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.module5_analytics.entity.ClassInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 班级 Mapper
 */
public interface ClassInfoMapper extends BaseMapper<ClassInfo> {

    /** 按教师ID查询班级列表 */
    List<ClassInfo> selectByTeacherId(@Param("teacherId") String teacherId);

    /** 检查同课程下班级名是否重复 */
    int countByNameAndCourse(@Param("name") String name,
                             @Param("courseId") String courseId,
                             @Param("excludeId") String excludeId);

    /** 查询班级内的学生ID列表 */
    List<String> selectStudentIdsByClassId(@Param("classId") String classId);

    /** 查询班级内的完整学生信息 */
    List<Student> selectStudentsByClassId(@Param("classId") String classId);

    /** 添加学生到班级 */
    int insertClassStudent(@Param("classId") String classId,
                           @Param("studentId") String studentId);

    /** 从班级移除学生 */
    int deleteClassStudent(@Param("classId") String classId,
                           @Param("studentId") String studentId);

    /** 统计班级学生数 */
    int countStudentsByClassId(@Param("classId") String classId);
}

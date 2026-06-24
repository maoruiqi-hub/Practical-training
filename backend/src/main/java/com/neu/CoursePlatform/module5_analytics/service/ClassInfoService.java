package com.neu.CoursePlatform.module5_analytics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.module5_analytics.entity.ClassInfo;

import java.util.List;

/**
 * 班级管理 Service 接口
 */
public interface ClassInfoService extends IService<ClassInfo> {

    /**
     * 按教师查询班级列表
     */
    List<ClassInfo> listByTeacher(String teacherId);

    /**
     * 创建班级（含同名校验）
     * @return 创建的班级，失败返回 null
     */
    ClassInfo createClass(ClassInfo classInfo);

    /**
     * 修改班级信息（含同名校验）
     * @return 修改后的班级，失败返回 null
     */
    ClassInfo updateClass(String id, ClassInfo classInfo);

    /**
     * 删除班级（有学生时拒绝）
     * @return true=删除成功，false=有学生或不存在
     */
    boolean deleteClassIfEmpty(String id);

    /**
     * 添加学生到班级
     * @return true=成功，false=已存在或失败
     */
    boolean enrollStudent(String classId, String studentId);

    /**
     * 从班级移除学生
     * @return true=成功
     */
    boolean removeStudent(String classId, String studentId);

    /**
     * 获取班级内学生ID列表
     */
    List<String> getStudentIds(String classId);

    /**
     * 班级学生数
     */
    int countStudents(String classId);
}

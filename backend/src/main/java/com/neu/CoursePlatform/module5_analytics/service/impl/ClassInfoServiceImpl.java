package com.neu.CoursePlatform.module5_analytics.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.module5_analytics.entity.ClassInfo;
import com.neu.CoursePlatform.module5_analytics.mapper.ClassInfoMapper;
import com.neu.CoursePlatform.module5_analytics.service.ClassInfoService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClassInfoServiceImpl
        extends ServiceImpl<ClassInfoMapper, ClassInfo>
        implements ClassInfoService {

    @Override
    public List<ClassInfo> listByTeacher(String teacherId) {
        return baseMapper.selectByTeacherId(teacherId);
    }

    @Override
    public ClassInfo createClass(ClassInfo classInfo) {
        // R1.8: 同课程下班级名称不可重复
        int count = baseMapper.countByNameAndCourse(
                classInfo.getName(), classInfo.getCourseId(), null);
        if (count > 0) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        classInfo.setCreatedAt(now);
        classInfo.setUpdatedAt(now);
        save(classInfo);
        return classInfo;
    }

    @Override
    public ClassInfo updateClass(String id, ClassInfo updated) {
        ClassInfo existing = getById(id);
        if (existing == null) {
            return null;
        }
        // R1.8: 改名时检查是否与同课程其他班级重名
        if (updated.getName() != null && !updated.getName().equals(existing.getName())) {
            int count = baseMapper.countByNameAndCourse(
                    updated.getName(), existing.getCourseId(), id);
            if (count > 0) {
                return null;
            }
        }
        updated.setId(id);
        updated.setUpdatedAt(LocalDateTime.now());
        // 只更新非 null 字段
        updateById(updated);
        return getById(id);
    }

    @Override
    public boolean deleteClassIfEmpty(String id) {
        // R1.7: 班级中有学生时不可删除
        int studentCount = baseMapper.countStudentsByClassId(id);
        if (studentCount > 0) {
            return false;
        }
        return removeById(id);
    }

    @Override
    public boolean enrollStudent(String classId, String studentId) {
        try {
            baseMapper.insertClassStudent(classId, studentId);
            return true;
        } catch (Exception e) {
            // 重复插入（唯一约束冲突）视为已存在
            return false;
        }
    }

    @Override
    public boolean removeStudent(String classId, String studentId) {
        return baseMapper.deleteClassStudent(classId, studentId) > 0;
    }

    @Override
    public List<String> getStudentIds(String classId) {
        return baseMapper.selectStudentIdsByClassId(classId);
    }

    @Override
    public int countStudents(String classId) {
        return baseMapper.countStudentsByClassId(classId);
    }
}

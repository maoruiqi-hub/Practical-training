package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.mapper.TeacherMapper;
import com.neu.CoursePlatform.service.TeacherService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements TeacherService {

    @Override
    public Teacher login(String username, String password) {
        if (username == null || username.isBlank() || password == null) {
            return null;
        }
        Teacher teacher = baseMapper.selectByUsername(username);
        if (teacher != null && teacher.getPassword().equals(password)) {
            return teacher;
        }
        return null;
    }

    @Override
    @Transactional
    public boolean register(Teacher teacher) {
        if (teacher == null || isBlank(teacher.getUsername())) {
            return false;
        }
        if (baseMapper.selectByUsername(teacher.getUsername()) != null) {
            return false;
        }
        try {
            return save(teacher);
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    @Override
    public List<Teacher> searchByKeyword(String keyword) {
        return baseMapper.selectByKeyword(keyword);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

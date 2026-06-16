package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.mapper.TeacherMapper;
import com.neu.CoursePlatform.service.TeacherService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements TeacherService {

    @Override
    public Teacher login(String username, String password) {
        Teacher teacher = baseMapper.selectByUsername(username);
        if (teacher != null && teacher.getPassword().equals(password)) {
            return teacher;
        }
        return null;
    }

    @Override
    public boolean register(Teacher teacher) {
        if (baseMapper.selectByUsername(teacher.getUsername()) != null) {
            return false;
        }
        return save(teacher);
    }

    @Override
    public List<Teacher> searchByKeyword(String keyword) {
        return baseMapper.selectByKeyword(keyword);
    }
}

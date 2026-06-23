package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.Student;
import java.util.List;
import com.neu.CoursePlatform.mapper.StudentMapper;
import com.neu.CoursePlatform.service.StudentService;
import org.springframework.stereotype.Service;

@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    @Override
    public Student login(String username, String password) {
        Student student = baseMapper.selectByUsername(username);
        if (student != null && student.getPassword().equals(password)) {
            return student;
        }
        return null;
    }

    @Override
    public boolean register(Student student) {
        if (baseMapper.selectByUsername(student.getUsername()) != null) {
            return false;
        }
        return save(student);
    }

    @Override
    public List<Student> searchByKeyword(String keyword) {
        return baseMapper.selectByKeyword(keyword);
    }
}

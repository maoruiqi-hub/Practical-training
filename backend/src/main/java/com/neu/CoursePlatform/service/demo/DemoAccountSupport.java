package com.neu.CoursePlatform.service.demo;

import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.mapper.StudentMapper;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DemoAccountSupport {
    private static final String DEMO_USERNAME = "dangshenghang";

    private final StudentMapper studentMapper;

    public DemoAccountSupport(StudentMapper studentMapper) {
        this.studentMapper = studentMapper;
    }

    public boolean isDemoStudent(String studentNo) {
        if (studentNo == null || studentNo.isBlank()) return false;
        Student student = studentMapper.selectById(studentNo);
        return student != null && Objects.equals(DEMO_USERNAME, student.getUsername());
    }
}

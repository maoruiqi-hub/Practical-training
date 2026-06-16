package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.Student;

public interface StudentService extends IService<Student> {

    Student login(String username, String password);

    boolean register(Student student);
}

package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.Student;
import java.util.List;

public interface StudentService extends IService<Student> {

    Student login(String username, String password);

    boolean register(Student student);

    List<Student> searchByKeyword(String keyword);
}

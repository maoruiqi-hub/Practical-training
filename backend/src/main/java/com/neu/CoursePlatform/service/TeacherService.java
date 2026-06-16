package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.Teacher;

import java.util.List;

public interface TeacherService extends IService<Teacher> {

    Teacher login(String username, String password);

    boolean register(Teacher teacher);

    List<Teacher> searchByKeyword(String keyword);
}

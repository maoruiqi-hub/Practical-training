package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.Student;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface StudentService extends IService<Student> {

    Student login(String username, String password);

    boolean register(Student student);

    List<Student> searchByKeyword(String keyword);

    List<Student> listByClassId(String classId);

    int importFromExcel(MultipartFile file) throws IOException;

    void exportToExcel(OutputStream out) throws IOException;
}

package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.mapper.StudentMapper;
import com.neu.CoursePlatform.service.StudentService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;

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

    @Override
    public List<Student> listByClassId(String classId) {
        if (classId == null || classId.isBlank()) {
            return list();
        }
        return baseMapper.selectByClassId(classId);
    }

    @Override
    public int importFromExcel(MultipartFile file) throws IOException {
        int count = 0;
        StringBuilder errors = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String header = reader.readLine(); // skip header
            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                try {
                    String[] cols = line.split(",");
                    if (cols.length < 6) {
                        errors.append("第").append(rowNum).append("行: 列数不足(需>=6列), 已跳过; ");
                        continue;
                    }
                    String studentNo = cols[0].trim();
                    if (baseMapper.selectById(studentNo) != null) {
                        errors.append("第").append(rowNum).append("行: 学号").append(studentNo).append("已存在, 已跳过; ");
                        continue;
                    }
                    Student s = new Student();
                    s.setStudentNo(studentNo);
                    s.setName(cols[1].trim());
                    s.setCollege(cols[2].trim());
                    s.setClassName(cols[3].trim());
                    s.setUsername(cols[4].trim());
                    s.setPassword(cols[5].trim());
                    s.setPhone(cols.length > 6 ? cols[6].trim() : "");
                    save(s);
                    count++;
                } catch (Exception e) {
                    errors.append("第").append(rowNum).append("行: 格式错误, 已跳过; ");
                }
            }
        }
        if (errors.length() > 0) {
            throw new IOException(errors.toString());
        }
        return count;
    }

    @Override
    public void exportToExcel(OutputStream out) throws IOException {
        List<Student> students = list();
        StringBuilder sb = new StringBuilder("姓名,学院,班级,用户名\n");
        for (Student s : students) {
            sb.append(String.join(",",
                s.getName(), s.getCollege(), s.getClassName(), s.getUsername()
            )).append("\n");
        }
        out.write(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.flush();
    }
}

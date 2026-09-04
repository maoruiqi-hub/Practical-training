package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.mapper.StudentMapper;
import com.neu.CoursePlatform.service.StudentService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.*;
import java.util.List;

@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Student login(String username, String password) {
        if (username == null || username.isBlank() || password == null) {
            return null;
        }
        Student student = baseMapper.selectByUsername(username);
        if (student != null && passwordMatches(password, student.getPassword())) {
            upgradeLegacyPassword(student, password);
            return student;
        }
        return null;
    }

    @Override
    @Transactional
    public boolean register(Student student) {
        if (student == null || isBlank(student.getUsername()) || isBlank(student.getPassword()) || isBlank(student.getName())) {
            return false;
        }
        if (baseMapper.selectByUsername(student.getUsername()) != null) {
            return false;
        }
        try {
            student.setPassword(passwordEncoder.encode(student.getPassword()));
            return save(student);
        } catch (DuplicateKeyException e) {
            return false;
        }
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
                    String rawPassword = cols[5].trim();
                    if (rawPassword.isBlank()) {
                        errors.append("第").append(rowNum).append("行: 密码不能为空, 已跳过; ");
                        continue;
                    }
                    s.setPassword(passwordEncoder.encode(rawPassword));
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    @Transactional
    public boolean updateById(Student student) {
        if (student == null || student.getStudentNo() == null) return false;
        if (isBlank(student.getPassword())) {
            Student existing = baseMapper.selectById(student.getStudentNo());
            if (existing != null) student.setPassword(existing.getPassword());
        } else if (!isBcrypt(student.getPassword())) {
            student.setPassword(passwordEncoder.encode(student.getPassword()));
        }
        return baseMapper.updateById(student) > 0;
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null) return false;
        return isBcrypt(storedPassword)
                ? passwordEncoder.matches(rawPassword, storedPassword)
                : storedPassword.equals(rawPassword);
    }

    private void upgradeLegacyPassword(Student student, String rawPassword) {
        if (isBcrypt(student.getPassword())) return;
        student.setPassword(passwordEncoder.encode(rawPassword));
        baseMapper.updateById(student);
    }

    private boolean isBcrypt(String value) {
        return value != null && value.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }
}

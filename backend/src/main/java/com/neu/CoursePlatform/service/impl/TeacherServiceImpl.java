package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.mapper.TeacherMapper;
import com.neu.CoursePlatform.service.TeacherService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements TeacherService {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Teacher login(String username, String password) {
        if (username == null || username.isBlank() || password == null) {
            return null;
        }
        Teacher teacher = baseMapper.selectByUsername(username);
        if (teacher != null && passwordMatches(password, teacher.getPassword())) {
            upgradeLegacyPassword(teacher, password);
            return teacher;
        }
        return null;
    }

    @Override
    @Transactional
    public boolean register(Teacher teacher) {
        if (teacher == null || isBlank(teacher.getUsername()) || isBlank(teacher.getPassword())) {
            return false;
        }
        if (baseMapper.selectByUsername(teacher.getUsername()) != null) {
            return false;
        }
        try {
            teacher.setPassword(passwordEncoder.encode(teacher.getPassword()));
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

    @Override
    @Transactional
    public boolean updateById(Teacher teacher) {
        if (teacher == null || teacher.getTeacherNo() == null) return false;
        if (isBlank(teacher.getPassword())) {
            Teacher existing = baseMapper.selectById(teacher.getTeacherNo());
            if (existing != null) teacher.setPassword(existing.getPassword());
        } else if (!isBcrypt(teacher.getPassword())) {
            teacher.setPassword(passwordEncoder.encode(teacher.getPassword()));
        }
        return baseMapper.updateById(teacher) > 0;
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null) return false;
        return isBcrypt(storedPassword)
                ? passwordEncoder.matches(rawPassword, storedPassword)
                : storedPassword.equals(rawPassword);
    }

    private void upgradeLegacyPassword(Teacher teacher, String rawPassword) {
        if (isBcrypt(teacher.getPassword())) return;
        teacher.setPassword(passwordEncoder.encode(rawPassword));
        baseMapper.updateById(teacher);
    }

    private boolean isBcrypt(String value) {
        return value != null && value.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }
}

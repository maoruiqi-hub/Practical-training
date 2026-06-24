package com.neu.CoursePlatform.common;

import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.service.CourseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class Auth {

    private final CourseService courseService;

    public Auth(CourseService courseService) {
        this.courseService = courseService;
    }

    public boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("student") != null
                || session.getAttribute("teacher") != null;
    }

    public boolean isAdmin(HttpSession session) {
        Teacher t = getTeacher(session);
        return t != null && "admin".equals(t.getRole());
    }

    public Teacher getTeacher(HttpSession session) {
        return (Teacher) session.getAttribute("teacher");
    }

    public Student getStudent(HttpSession session) {
        return (Student) session.getAttribute("student");
    }

    public boolean canModifyCourse(HttpSession session, String courseCode) {
        Teacher t = getTeacher(session);
        if (t == null) return false;
        if ("admin".equals(t.getRole())) return true;
        Course c = courseService.getById(courseCode);
        if (c == null) return false;
        // teacherNo is the stable ownership key. The name fallback keeps old
        // course records usable until their teacher_no values are migrated.
        if (c.getTeacherNo() != null && !c.getTeacherNo().isBlank()) {
            return c.getTeacherNo().equals(t.getTeacherNo());
        }
        return t.getName().equals(c.getTeacher());
    }

    /**
     * 获取当前登录教师的 ID（字符串形式）。
     * 现在 Teacher 表用 INT，返回 String.valueOf(int)；
     * 未来迁移 UUID v4 后，返回 UUID 字符串。
     * 其他模块通过此方法获取教师 ID，不直接依赖 Teacher.getTeacherNo() 的类型。
     */
    public String getTeacherId(HttpSession session) {
        Teacher t = getTeacher(session);
        if (t == null) return null;
        return String.valueOf(t.getTeacherNo());
    }
}

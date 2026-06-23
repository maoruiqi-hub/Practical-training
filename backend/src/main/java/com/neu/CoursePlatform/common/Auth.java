package com.neu.CoursePlatform.common;

import com.neu.CoursePlatform.entity.Course;
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

    public boolean canModifyCourse(HttpSession session, String courseCode) {
        Teacher t = getTeacher(session);
        if (t == null) return false;
        if ("admin".equals(t.getRole())) return true;
        Course c = courseService.getById(courseCode);
        return c != null && t.getName().equals(c.getTeacher());
    }
}

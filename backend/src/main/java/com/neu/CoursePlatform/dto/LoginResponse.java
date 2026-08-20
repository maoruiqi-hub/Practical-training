package com.neu.CoursePlatform.dto;

import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.Teacher;
import lombok.Data;

/** 登录响应对象，只暴露前端登录后需要的非敏感字段。 */
@Data
public class LoginResponse {
    private String studentNo;
    private String teacherNo;
    private String name;
    private String college;
    private String major;
    private String className;
    private String courseGrades;
    private String phone;
    private String username;
    private String role;

    public static LoginResponse fromStudent(Student student) {
        LoginResponse response = new LoginResponse();
        response.studentNo = student.getStudentNo();
        response.name = student.getName();
        response.college = student.getCollege();
        response.className = student.getClassName();
        response.courseGrades = student.getCourseGrades();
        response.phone = student.getPhone();
        response.username = student.getUsername();
        response.role = "student";
        return response;
    }

    public static LoginResponse fromTeacher(Teacher teacher) {
        LoginResponse response = new LoginResponse();
        response.teacherNo = teacher.getTeacherNo();
        response.name = teacher.getName();
        response.college = teacher.getCollege();
        response.major = teacher.getMajor();
        response.phone = teacher.getPhone();
        response.username = teacher.getUsername();
        response.role = teacher.getRole();
        return response;
    }
}

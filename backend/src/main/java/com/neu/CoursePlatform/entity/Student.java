package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
public class Student {
    /** 学号 */
    @TableId(type = IdType.AUTO)
    private String studentNo;
    /** 姓名 */
    private String name;
    /** 学院 */
    private String college;
    /** 班级 */
    private String className;
    /** 课程成绩 */
    private String courseGrades;
    /** 用户名 */
    private String username;
    /** 密码 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    /** 联系方式 */
    private String phone;
}

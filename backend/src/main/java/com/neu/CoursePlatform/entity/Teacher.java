package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Teacher {
    /** 教职工码 */
    @TableId(type = IdType.AUTO)
    private String teacherNo;
    /** 姓名 */
    private String name;
    /** 学院 */
    private String college;
    /** 专业（系） */
    private String major;
    /** 联系电话 */
    private String phone;
    /** 角色：teacher / admin */
    private String role;
    /** 用户名 */
    private String username;
    /** 密码 */
    private String password;
}

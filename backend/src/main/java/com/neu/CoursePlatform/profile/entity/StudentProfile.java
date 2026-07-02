package com.neu.CoursePlatform.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StudentProfile {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private Integer studentNo;
    private Integer courseCode;
    private Integer hp;
    private Integer atk;
    private Integer def;
    private Integer exp;
    private Integer level;
    private Integer coins;
    private Integer energy;
    private String status;
    private Integer consecutiveCorrect;
    private String recentAnswers;
    private String recentScores;
    private java.util.Date lastActivityDate;
    private java.util.Date updatedAt;
}

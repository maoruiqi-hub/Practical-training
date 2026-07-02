package com.neu.CoursePlatform.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Achievement {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private Integer studentNo;
    private Integer courseCode;
    private String achievementType;
    private String name;
    private String description;
    private java.util.Date earnedAt;
    private String metadata;
}

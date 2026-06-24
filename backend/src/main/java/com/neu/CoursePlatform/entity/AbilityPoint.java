package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class AbilityPoint {
    @TableId(value = "ability_point_id", type = IdType.AUTO)
    private String abilityPointId;
    private String courseCode;
    private String name;
    private String description;
}

package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class CompetencyPoint {
    @TableId(value = "competency_id", type = IdType.INPUT)
    private String competencyId;
    private String courseCode;
    private String name;
    private String description;
    private String status;
    private Integer sortOrder;
}

package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AbilityPointCompetencyRelation {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String courseCode;
    private String abilityPointId;
    private String competencyId;
    private String relationStatus;
    private BigDecimal strength;
    private BigDecimal confidence;
    private String strengthSource;
    private Integer evidenceCount;
    private String matrixVersion;
    private String reviewNote;
    private LocalDateTime updatedAt;
}

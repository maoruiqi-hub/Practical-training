package com.neu.CoursePlatform.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CompetencyScore {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private Integer studentNo;
    private Integer courseCode;
    private String abilityPointId;
    private String abilityPointName;
    private Integer score;
    private java.util.Date lastUpdated;
}

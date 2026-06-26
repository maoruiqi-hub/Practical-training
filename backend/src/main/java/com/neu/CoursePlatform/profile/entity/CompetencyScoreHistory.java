package com.neu.CoursePlatform.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CompetencyScoreHistory {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private Integer studentNo;
    private Integer courseCode;
    private String abilityPointId;
    private Integer oldScore;
    private Integer newScore;
    private String changeReason;
    private java.util.Date changedAt;
}

package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class AbilityKnowledgePoint {
    @TableId(type = IdType.AUTO)
    private String id;
    private String abilityPointId;
    private String knowledgePointId;
}

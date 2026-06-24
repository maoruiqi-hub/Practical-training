package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class KnowledgePoint {

    @TableId(value = "knowledge_point_id", type = IdType.AUTO)
    private String knowledgePointId;

    private String courseCode;
    private String name;
    private String description;
    private String chapter;
    private Integer importance;
    private String generationMethod;

    @TableField(exist = false)
    private List<KnowledgePoint> children;
}

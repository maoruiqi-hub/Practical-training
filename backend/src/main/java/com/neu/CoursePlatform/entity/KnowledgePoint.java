package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识点实体
 */
@Data
@NoArgsConstructor
public class KnowledgePoint {
    @TableId(type = IdType.AUTO)
    private String knowledgePointId;
    private String courseCode;
    private String lessonNo;
    private String name;
    private String description;
}
